package com.raikiservices.backend.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.blog.BlogPostRequest;
import com.raikiservices.backend.dto.blog.BlogPostResponse;
import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.notification.SseEvent;
import com.raikiservices.backend.entity.BlogPost;
import com.raikiservices.backend.entity.BlogStatus;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.BlogPostRepository;
import com.raikiservices.backend.repository.UserRepository;

@Service
public class BlogService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of(
            "title", "slug", "category", "status", "publishedAt", "createdAt", "updatedAt");

    private final BlogPostRepository blogPostRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BlogService(BlogPostRepository blogPostRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.blogPostRepository = blogPostRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ──────────────── Administration ────────────────

    @Transactional(readOnly = true)
    public PageResponse<BlogPostResponse> getAll(PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, "createdAt");
        String filter = query.filterOrNull();

        Page<BlogPost> result = filter == null
                ? blogPostRepository.findAll(pageable)
                : blogPostRepository.search(filter, pageable);

        return PageResponse.from(result, BlogPostResponse::summaryFrom);
    }

    @Transactional(readOnly = true)
    public BlogPostResponse getById(Long id) {
        return BlogPostResponse.from(findOrThrow(id));
    }

    @Transactional
    public BlogPostResponse create(BlogPostRequest request) {
        BlogPost post = new BlogPost();
        apply(post, request);
        post.setSlug(resolveSlug(request.slug(), request.title(), null));

        BlogPost saved = blogPostRepository.save(post);
        announceIfNewlyPublished(saved, false);
        return BlogPostResponse.from(saved);
    }

    @Transactional
    public BlogPostResponse update(Long id, BlogPostRequest request) {
        BlogPost post = findOrThrow(id);
        boolean wasPublished = post.getStatus() == BlogStatus.PUBLISHED;

        apply(post, request);
        post.setSlug(resolveSlug(request.slug(), request.title(), post.getId()));

        BlogPost saved = blogPostRepository.save(post);
        announceIfNewlyPublished(saved, wasPublished);
        return BlogPostResponse.from(saved);
    }

    /**
     * Notifie la mise en ligne, et seulement au passage de brouillon a publie : sans ce
     * garde-fou, chaque enregistrement d'un article deja publie renotifierait tout le monde.
     */
    private void announceIfNewlyPublished(BlogPost post, boolean wasPublished) {
        if (wasPublished || post.getStatus() != BlogStatus.PUBLISHED) {
            return;
        }
        notificationService.dispatch(new SseEvent(
                NotificationType.BLOG_PUBLISHED.getCode(),
                "Article publie",
                post.getTitle(),
                post.getId()));
    }

    @Transactional
    public void delete(Long id) {
        if (!blogPostRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Article", id);
        }
        blogPostRepository.deleteById(id);
    }

    // ──────────────── Site public (articles publiés uniquement) ────────────────

    @Transactional(readOnly = true)
    public PageResponse<BlogPostResponse> getPublished(PageQuery query) {
        Pageable pageable = query.toPageable(SORTABLE, "publishedAt");
        String filter = query.filterOrNull();

        Page<BlogPost> result = filter == null
                ? blogPostRepository.findByStatus(BlogStatus.PUBLISHED, pageable)
                : blogPostRepository.searchByStatus(filter, BlogStatus.PUBLISHED, pageable);

        return PageResponse.from(result, BlogPostResponse::summaryFrom);
    }

    /**
     * Un brouillon reste introuvable côté public : renvoyer 404 plutôt que 403 évite de
     * révéler qu'un article existe à cette adresse avant sa publication.
     */
    @Transactional(readOnly = true)
    public BlogPostResponse getPublishedBySlug(String slug) {
        return blogPostRepository.findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
                .map(BlogPostResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", slug));
    }

    // ──────────────── Interne ────────────────

    private void apply(BlogPost post, BlogPostRequest request) {
        post.setTitle(request.title());
        post.setExcerpt(request.excerpt());
        post.setContent(request.content());
        post.setCoverImage(request.coverImage());
        post.setCategory(request.category());

        if (request.tags() != null) {
            post.setTags(request.tags());
        }

        if (request.authorId() != null) {
            User author = userRepository.findById(request.authorId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Auteur", request.authorId()));
            post.setAuthor(author);
        } else {
            post.setAuthor(null);
        }

        BlogStatus newStatus = request.status() != null ? request.status() : BlogStatus.DRAFT;
        // Horodate la première publication ; repasser en brouillon efface la date, de sorte
        // qu'une republication ultérieure reflète bien la nouvelle mise en ligne.
        if (newStatus == BlogStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        } else if (newStatus == BlogStatus.DRAFT) {
            post.setPublishedAt(null);
        }
        post.setStatus(newStatus);
    }

    private BlogPost findOrThrow(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
    }

    /**
     * Produit un slug unique à partir du slug fourni, ou du titre à défaut. En cas de
     * collision, suffixe par un compteur — {@code mon-article}, {@code mon-article-2}…
     *
     * @param selfId article en cours de modification, exclu du test de collision
     */
    private String resolveSlug(String providedSlug, String title, Long selfId) {
        String base = slugify((providedSlug != null && !providedSlug.isBlank()) ? providedSlug : title);
        if (base.isEmpty()) {
            base = "article";
        }

        String candidate = base;
        int counter = 2;
        while (isSlugTaken(candidate, selfId)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, Long selfId) {
        return blogPostRepository.findBySlug(slug)
                .map(existing -> !existing.getId().equals(selfId))
                .orElse(false);
    }

    /** « Été à Paris ! » devient « ete-a-paris ». */
    private String slugify(String input) {
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return slug.toLowerCase(Locale.ROOT);
    }
}

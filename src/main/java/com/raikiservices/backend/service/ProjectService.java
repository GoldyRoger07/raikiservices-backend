package com.raikiservices.backend.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.project.ProjectReorderRequest;
import com.raikiservices.backend.dto.project.ProjectRequest;
import com.raikiservices.backend.dto.project.ProjectResponse;
import com.raikiservices.backend.entity.Project;
import com.raikiservices.backend.entity.ProjectStatus;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.ProjectRepository;

/**
 * Réalisations du site vitrine.
 *
 * <p>Même contrat que le blog — liste paginée, détail, création, modification, suppression,
 * et une face publique qui ne voit que le publié. S'y ajoute le réordonnancement : une
 * vitrine se range à la main, pas par date.
 */
@Service
public class ProjectService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of(
            "title", "slug", "clientLabel", "sector", "status", "displayOrder",
            "publishedAt", "createdAt", "updatedAt");

    /** Rang d'affichage : l'ordre naturel d'une vitrine, côté public comme au back-office. */
    private static final String DEFAULT_SORT = "displayOrder";

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // ──────────────── Administration ────────────────

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getAll(PageQuery query) {
        Pageable pageable = pageable(query);
        String filter = query.filterOrNull();

        Page<Project> result = filter == null
                ? projectRepository.findAll(pageable)
                : projectRepository.search(filter, pageable);

        return PageResponse.from(result, ProjectResponse::summaryFrom);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return ProjectResponse.from(findOrThrow(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        apply(project, request);
        project.setSlug(resolveSlug(request.slug(), request.title(), null));

        // Sans rang explicite, le projet se range en fin de vitrine : le créer ne doit pas
        // bousculer l'ordre déjà réglé à la main.
        project.setDisplayOrder(request.displayOrder() != null
                ? request.displayOrder()
                : projectRepository.findMaxDisplayOrder() + 1);

        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findOrThrow(id);

        apply(project, request);
        project.setSlug(resolveSlug(request.slug(), request.title(), project.getId()));

        if (request.displayOrder() != null) {
            project.setDisplayOrder(request.displayOrder());
        }

        return ProjectResponse.from(projectRepository.save(project));
    }

    /**
     * Applique un nouvel ordre à la vitrine.
     *
     * <p>Les rangs ne sont pas renumérotés de zéro : on relève ceux que les projets visés
     * occupent déjà, on les trie, et on les redistribue dans l'ordre demandé. Les projets
     * permutent donc entre leurs propres places, sans toucher au reste de la vitrine —
     * c'est ce qui permet à l'administration de réordonner la page affichée sans écraser
     * silencieusement l'ordre des pages suivantes.
     *
     * <p>Les identifiants inconnus sont ignorés plutôt que rejetés : un projet supprimé
     * entre-temps depuis un autre onglet ne doit pas faire échouer le rangement des autres.
     *
     * @return le nombre de projets effectivement repositionnés
     */
    @Transactional
    public int reorder(ProjectReorderRequest request) {
        List<Long> ids = request.ids();
        Map<Long, Project> found = projectRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));

        List<Integer> slots = found.values().stream()
                .map(Project::getDisplayOrder)
                .sorted()
                .toList();

        int position = 0;
        for (Long id : ids) {
            Project project = found.get(id);
            if (project != null) {
                project.setDisplayOrder(slots.get(position++));
            }
        }
        projectRepository.saveAll(found.values());
        return position;
    }

    @Transactional
    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Projet", id);
        }
        projectRepository.deleteById(id);
    }

    // ──────────────── Site public (projets publiés uniquement) ────────────────

    /** Portfolio : toutes les réalisations publiées. */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getPublished(PageQuery query) {
        Pageable pageable = pageable(query);
        String filter = query.filterOrNull();

        Page<Project> result = filter == null
                ? projectRepository.findByStatus(ProjectStatus.PUBLISHED, pageable)
                : projectRepository.searchByStatus(filter, ProjectStatus.PUBLISHED, pageable);

        return PageResponse.from(result, ProjectResponse::summaryFrom);
    }

    /**
     * Études de cas : réponse complète et non résumée, la page publique affichant le texte
     * de chaque étude sans passer par une page de détail.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getCaseStudies(PageQuery query) {
        Page<Project> result = projectRepository
                .findByStatusAndCaseStudyTrue(ProjectStatus.PUBLISHED, pageable(query));

        return PageResponse.from(result, ProjectResponse::from);
    }

    /** Réalisations mises en avant sur l'accueil. */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getFeatured(PageQuery query) {
        Page<Project> result = projectRepository
                .findByStatusAndFeaturedTrue(ProjectStatus.PUBLISHED, pageable(query));

        return PageResponse.from(result, ProjectResponse::summaryFrom);
    }

    /**
     * Un brouillon reste introuvable côté public : renvoyer 404 plutôt que 403 évite de
     * révéler qu'un projet existe à cette adresse avant sa mise en ligne.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getPublishedBySlug(String slug) {
        return projectRepository.findBySlugAndStatus(slug, ProjectStatus.PUBLISHED)
                .map(ProjectResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of("Projet", slug));
    }

    // ──────────────── Interne ────────────────

    /**
     * Pagination des listes de projets.
     *
     * <p>Le rang d'affichage se répète d'un projet à l'autre — rien n'empêche deux
     * réalisations de partager le même. On départage par date de publication décroissante,
     * sans quoi l'ordre des ex æquo varierait d'une requête à l'autre et la pagination
     * pourrait montrer deux fois le même projet tout en en omettant un autre.
     */
    private Pageable pageable(PageQuery query) {
        Pageable base = query.toPageable(SORTABLE, DEFAULT_SORT);
        Sort sort = base.getSort();

        if (sort.getOrderFor("publishedAt") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "publishedAt"));
        }
        return PageRequest.of(base.getPageNumber(), base.getPageSize(), sort);
    }

    private void apply(Project project, ProjectRequest request) {
        project.setTitle(request.title());
        project.setClientLabel(request.clientLabel());
        project.setSummary(request.summary());
        project.setDescription(request.description());
        project.setIcon(request.icon());
        project.setWebsiteUrl(request.websiteUrl());
        project.setCoverPublicId(request.coverPublicId());
        project.setSector(request.sector());

        // Collections vidées puis regarnies plutôt que remplacées par un nouvel exemplaire :
        // Hibernate suit celui qu'il a lui-même attaché à l'entité.
        if (request.galleryPublicIds() != null) {
            project.getGalleryPublicIds().clear();
            project.getGalleryPublicIds().addAll(request.galleryPublicIds());
        }
        if (request.services() != null) {
            project.getServices().clear();
            project.getServices().addAll(request.services());
        }

        project.setCaseStudy(Boolean.TRUE.equals(request.caseStudy()));
        project.setFeatured(Boolean.TRUE.equals(request.featured()));

        ProjectStatus newStatus = request.status() != null ? request.status() : ProjectStatus.DRAFT;
        // Horodate la première mise en ligne ; repasser en brouillon efface la date, de sorte
        // qu'une republication ultérieure reflète bien la nouvelle publication.
        if (newStatus == ProjectStatus.PUBLISHED && project.getPublishedAt() == null) {
            project.setPublishedAt(LocalDateTime.now());
        } else if (newStatus == ProjectStatus.DRAFT) {
            project.setPublishedAt(null);
        }
        project.setStatus(newStatus);
    }

    private Project findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Projet", id));
    }

    /**
     * Produit un slug unique à partir du slug fourni, ou du titre à défaut. En cas de
     * collision, suffixe par un compteur — {@code michaels}, {@code michaels-2}…
     *
     * @param selfId projet en cours de modification, exclu du test de collision
     */
    private String resolveSlug(String providedSlug, String title, Long selfId) {
        String base = slugify((providedSlug != null && !providedSlug.isBlank()) ? providedSlug : title);
        if (base.isEmpty()) {
            base = "projet";
        }

        String candidate = base;
        int counter = 2;
        while (isSlugTaken(candidate, selfId)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private boolean isSlugTaken(String slug, Long selfId) {
        return projectRepository.findBySlug(slug)
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

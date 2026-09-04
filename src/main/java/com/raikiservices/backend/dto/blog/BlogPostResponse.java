package com.raikiservices.backend.dto.blog;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import com.raikiservices.backend.entity.BlogPost;
import com.raikiservices.backend.entity.BlogStatus;
import com.raikiservices.backend.entity.User;

public record BlogPostResponse(
        Long id,
        String title,
        String slug,
        String excerpt,
        String content,
        String coverImage,
        BlogStatus status,
        LocalDateTime publishedAt,
        Long authorId,
        String authorName,
        String authorPhotoUrl,
        String category,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt) {

    public static BlogPostResponse from(BlogPost post) {
        User author = post.getAuthor();
        return new BlogPostResponse(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getExcerpt(),
                post.getContent(),
                post.getCoverImage(),
                post.getStatus(),
                post.getPublishedAt(),
                author == null ? null : author.getId(),
                author == null ? null : displayName(author),
                author == null ? null : author.getPhotoUrl(),
                post.getCategory(),
                post.getTags(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    /**
     * Variante allégée pour les listes : le corps de l'article, potentiellement très long,
     * n'a pas à transiter dans une page de 10 résumés.
     */
    public static BlogPostResponse summaryFrom(BlogPost post) {
        BlogPostResponse full = from(post);
        return new BlogPostResponse(
                full.id(), full.title(), full.slug(), full.excerpt(), null, full.coverImage(),
                full.status(), full.publishedAt(), full.authorId(), full.authorName(),
                full.authorPhotoUrl(), full.category(), full.tags(), full.createdAt(), full.updatedAt());
    }

    private static String displayName(User author) {
        String first = author.getFirstName() == null ? "" : author.getFirstName();
        String last = author.getLastName() == null ? "" : author.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? author.getUsername() : full;
    }
}

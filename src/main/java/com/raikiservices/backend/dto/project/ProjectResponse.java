package com.raikiservices.backend.dto.project;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.raikiservices.backend.entity.Project;
import com.raikiservices.backend.entity.ProjectStatus;

public record ProjectResponse(
        Long id,
        String title,
        String slug,
        String clientLabel,
        String summary,
        String description,
        String icon,
        String websiteUrl,
        String coverPublicId,
        List<String> galleryPublicIds,
        Set<String> services,
        String sector,
        ProjectStatus status,
        boolean caseStudy,
        boolean featured,
        int displayOrder,
        LocalDateTime publishedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getSlug(),
                project.getClientLabel(),
                project.getSummary(),
                project.getDescription(),
                project.getIcon(),
                project.getWebsiteUrl(),
                project.getCoverPublicId(),
                List.copyOf(project.getGalleryPublicIds()),
                Set.copyOf(project.getServices()),
                project.getSector(),
                project.getStatus(),
                project.isCaseStudy(),
                project.isFeatured(),
                project.getDisplayOrder(),
                project.getPublishedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }

    /**
     * Variante allégée pour les listes.
     *
     * <p>La description, qui peut faire plusieurs paragraphes, et la galerie ne partent pas
     * dans une page de résumés ; les prestations, elles, sont conservées — les cartes du
     * portfolio les affichent directement depuis la liste.
     *
     * <p>Sert au tableau du back-office et au portfolio public. La liste des études de cas
     * fait exception et renvoie la réponse complète : la page affiche le texte de chaque
     * étude sans passer par une page de détail, et elles se comptent sur les doigts d'une
     * main.
     */
    public static ProjectResponse summaryFrom(Project project) {
        ProjectResponse full = from(project);
        return new ProjectResponse(
                full.id(), full.title(), full.slug(), full.clientLabel(), full.summary(),
                null, full.icon(), full.websiteUrl(), full.coverPublicId(), List.of(),
                full.services(), full.sector(), full.status(), full.caseStudy(), full.featured(),
                full.displayOrder(), full.publishedAt(), full.createdAt(), full.updatedAt());
    }
}

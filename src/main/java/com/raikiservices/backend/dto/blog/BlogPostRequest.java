package com.raikiservices.backend.dto.blog;

import java.util.Set;

import com.raikiservices.backend.entity.BlogStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param slug     optionnel : dérivé du titre s'il est absent, et suffixé si déjà pris
 * @param status   {@code DRAFT} par défaut
 * @param authorId compte auteur de l'article ; null pour un article sans auteur affiché
 */
public record BlogPostRequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères.")
        String title,

        String slug,
        String excerpt,
        String content,
        String coverImage,
        BlogStatus status,
        Long authorId,

        @Size(max = 100, message = "La catégorie ne peut pas dépasser 100 caractères.")
        String category,

        Set<String> tags) {
}

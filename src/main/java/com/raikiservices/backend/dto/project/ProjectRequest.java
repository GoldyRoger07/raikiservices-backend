package com.raikiservices.backend.dto.project;

import java.util.List;
import java.util.Set;

import com.raikiservices.backend.entity.ProjectStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Charge utile de création et de modification d'une réalisation.
 *
 * <p>Les champs enveloppés ({@code Boolean}, {@code Integer}) distinguent « non renseigné »
 * de « faux » ou « zéro » : absents, ils laissent le backend décider — les drapeaux
 * retombent à {@code false} et le rang d'affichage est conservé, ou attribué en fin de
 * vitrine à la création.
 *
 * @param slug         optionnel : dérivé du titre s'il est absent, et suffixé si déjà pris
 * @param status       {@code DRAFT} par défaut
 * @param displayOrder rang d'affichage ; laissé vide, il n'est pas touché
 */
public record ProjectRequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères.")
        String title,

        String slug,

        @Size(max = 255, message = "Le libellé client ne peut pas dépasser 255 caractères.")
        String clientLabel,

        String summary,
        String description,

        @Size(max = 100, message = "L'icône ne peut pas dépasser 100 caractères.")
        String icon,

        @Size(max = 500, message = "L'adresse du site ne peut pas dépasser 500 caractères.")
        String websiteUrl,

        @Size(max = 255, message = "L'identifiant de la couverture ne peut pas dépasser 255 caractères.")
        String coverPublicId,

        List<String> galleryPublicIds,
        Set<String> services,

        @Size(max = 100, message = "Le secteur ne peut pas dépasser 100 caractères.")
        String sector,

        ProjectStatus status,
        Boolean caseStudy,
        Boolean featured,
        Integer displayOrder) {
}

package com.raikiservices.backend.dto.media;

import jakarta.validation.constraints.Size;

/**
 * Seul champ modifiable d'une image : son texte alternatif. Tout le reste décrit le fichier
 * déposé chez ImageKit et n'a pas de sens à être réécrit ici.
 */
public record MediaUpdateRequest(
        @Size(max = 500, message = "Le texte alternatif ne peut pas dépasser 500 caractères.")
        String alt) {
}

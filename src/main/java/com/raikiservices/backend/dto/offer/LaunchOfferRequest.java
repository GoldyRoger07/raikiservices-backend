package com.raikiservices.backend.dto.offer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Réglage de l'offre de lancement.
 *
 * <p>Le nombre de places attribuées ne s'incrémente pas tout seul : aucun événement du
 * système ne sait ce qu'est un client signé. C'est une saisie, faite au moment où l'accord
 * est conclu.
 */
public record LaunchOfferRequest(
        @NotNull(message = "L'état de l'offre est obligatoire.")
        Boolean active,

        @NotNull(message = "Le nombre de places est obligatoire.")
        @Min(value = 1, message = "L'offre doit compter au moins une place.")
        @Max(value = 999, message = "Le nombre de places ne peut pas dépasser 999.")
        Integer totalSlots,

        @NotNull(message = "Le nombre de places attribuées est obligatoire.")
        @Min(value = 0, message = "Le nombre de places attribuées ne peut pas être négatif.")
        Integer claimedSlots) {
}

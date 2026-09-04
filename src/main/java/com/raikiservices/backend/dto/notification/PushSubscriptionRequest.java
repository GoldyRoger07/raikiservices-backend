package com.raikiservices.backend.dto.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Abonnement produit par l'API PushManager du navigateur, transmis tel quel.
 *
 * @param keys clés de chiffrement du client, indispensables au chiffrement du payload
 */
public record PushSubscriptionRequest(
        @NotBlank(message = "L'endpoint est obligatoire.")
        String endpoint,

        @NotNull(message = "Les clés d'abonnement sont obligatoires.")
        @Valid
        Keys keys) {

    public record Keys(
            @NotBlank(message = "La clé p256dh est obligatoire.")
            String p256dh,

            @NotBlank(message = "La clé auth est obligatoire.")
            String auth) {
    }
}

package com.raikiservices.backend.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Charge utile du formulaire public. Aucun champ de suivi n'est acceptable ici. */
public record ContactMessageRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères.")
        String firstName,

        @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères.")
        String lastName,

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        @Size(max = 255, message = "L'adresse email ne peut pas dépasser 255 caractères.")
        String email,

        @Size(max = 30, message = "Le numéro de téléphone ne peut pas dépasser 30 caractères.")
        String phone,

        @Size(max = 150, message = "Le nom de société ne peut pas dépasser 150 caractères.")
        String companyName,

        @Size(max = 100, message = "La prestation ne peut pas dépasser 100 caractères.")
        String serviceCategory,

        @Size(max = 200, message = "Le sujet ne peut pas dépasser 200 caractères.")
        String subject,

        @NotBlank(message = "Le message est obligatoire.")
        @Size(max = 5000, message = "Le message ne peut pas dépasser 5000 caractères.")
        String message,

        Boolean newsletterOptIn) {

    /**
     * Normalise la case a cocher absente en {@code false}.
     *
     * <p>Un formulaire HTML n'envoie pas de case decochee : declarer un {@code boolean}
     * primitif ferait echouer la deserialisation en 400 dans ce cas pourtant legitime.
     */
    public ContactMessageRequest {
        newsletterOptIn = newsletterOptIn != null && newsletterOptIn;
    }
}

package com.raikiservices.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corps commun au renvoi de code de vérification et à la demande de réinitialisation. */
public record EmailRequest(
        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        String email) {
}

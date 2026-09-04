package com.raikiservices.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire.")
        @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit faire entre 3 et 50 caractères.")
        String username,

        @NotBlank(message = "L'adresse email est obligatoire.")
        @Email(message = "L'adresse email n'est pas valide.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, max = 100, message = "Le mot de passe doit faire au moins 8 caractères.")
        String password,

        String firstName,
        String lastName) {
}

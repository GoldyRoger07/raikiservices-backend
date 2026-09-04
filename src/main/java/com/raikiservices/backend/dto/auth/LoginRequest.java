package com.raikiservices.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * @param login    email ou nom d'utilisateur
 * @param password mot de passe en clair
 */
public record LoginRequest(
        @NotBlank(message = "L'identifiant est obligatoire.") String login,
        @NotBlank(message = "Le mot de passe est obligatoire.") String password) {
}

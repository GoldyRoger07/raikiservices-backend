package com.raikiservices.backend.dto.auth;

/** Réponse neutre des endpoints qui ne doivent rien révéler sur l'existence d'un compte. */
public record MessageResponse(String message) {
}

package com.raikiservices.backend.exception;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corps d'erreur uniforme de l'API.
 *
 * @param fieldErrors erreurs de validation par champ, omis quand il n'y en a pas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, null);
    }

    public static ApiError validation(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, "Bad Request", message, fieldErrors);
    }
}

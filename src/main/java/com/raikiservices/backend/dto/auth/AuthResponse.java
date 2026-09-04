package com.raikiservices.backend.dto.auth;

/**
 * Réponse de connexion / rafraîchissement.
 *
 * <p>Le refresh token n'apparaît pas ici : il est déposé dans un cookie HttpOnly,
 * hors de portée du JavaScript du frontend.
 *
 * @param expiresIn durée de validité de l'access token, en secondes
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user) {

    public static AuthResponse of(String accessToken, long expiresIn, UserSummaryResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, user);
    }
}

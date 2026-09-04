package com.raikiservices.backend.security;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Fabrique le cookie porteur du refresh token.
 *
 * <p>HttpOnly : inaccessible au JavaScript, ce qui neutralise le vol par XSS.
 * Le chemin est restreint à {@code /api/v1/auth}, donc le cookie n'accompagne pas les
 * appels métier — seulement le rafraîchissement et la déconnexion.
 *
 * <p>{@code app.cookie.same-site} vaut {@code Strict} par défaut, ce qui suppose que le
 * frontend et l'API partagent le même domaine enregistrable (raikiservices.com et
 * api.raikiservices.com, par exemple). Si le frontend est hébergé sur un domaine différent,
 * il faut passer à {@code None} — et alors {@code app.cookie.secure} doit être {@code true},
 * les navigateurs refusant {@code SameSite=None} sans {@code Secure}.
 */
@Component
public class RefreshTokenCookieFactory {

    public static final String COOKIE_NAME = "refresh_token";
    private static final String COOKIE_PATH = "/api/v1/auth";

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeSeconds;

    public RefreshTokenCookieFactory(
            @Value("${app.cookie.secure:true}") boolean secure,
            @Value("${app.cookie.same-site:Strict}") String sameSite,
            JwtService jwtService) {

        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeSeconds = jwtService.getRefreshExpirationSeconds();

        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException(
                    "app.cookie.same-site=None exige app.cookie.secure=true : "
                            + "les navigateurs rejettent silencieusement un tel cookie.");
        }
    }

    public ResponseCookie create(String refreshToken) {
        return base(refreshToken).maxAge(Duration.ofSeconds(maxAgeSeconds)).build();
    }

    /** Cookie d'expiration immédiate, utilisé à la déconnexion. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH);
    }

    /** Lit le refresh token dans la requête, ou {@code null} si le cookie est absent. */
    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

package com.raikiservices.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Génération et vérification des JWT.
 *
 * <p>Deux types de jetons sont émis, distingués par le claim {@code type} :
 * <ul>
 *   <li>{@code access} — court (jwt.expiration), renvoyé dans le corps de la réponse ;</li>
 *   <li>{@code refresh} — long (jwt.refresh-expiration), déposé dans un cookie HttpOnly.</li>
 * </ul>
 * Le claim {@code type} empêche qu'un refresh token soit présenté comme un access token.
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "type";
    /** Session a laquelle l'access token est rattache, cf. RefreshToken.jti. */
    private static final String CLAIM_SESSION = "sid";

    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret doit faire au moins 32 caractères (256 bits) pour HS256, "
                            + "valeur actuelle : " + keyBytes.length + " octets.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String subject) {
        return generateAccessToken(subject, null);
    }

    /**
     * Access token portant l'identifiant de la session qui l'a emis.
     *
     * <p>Le cookie de refresh etant limite au chemin d'authentification, c'est le seul moyen
     * pour les autres endpoints — la liste des sessions, notamment — de savoir depuis quelle
     * session la requete arrive.
     */
    public String generateAccessToken(String subject, String sessionJti) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessExpirationMs)));

        if (sessionJti != null) {
            builder.claim(CLAIM_SESSION, sessionJti);
        }
        return builder.signWith(key).compact();
    }

    /** Session portee par un access token valide, ou {@code null}. */
    public String extractSessionId(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }
            return claims.get(CLAIM_SESSION, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Refresh token signe, porteur d'un identifiant unique ({@code jti}).
     *
     * @param jti       identifiant a retrouver en base pour revoquer la session
     * @param expiresAt instant d'expiration, a stocker avec la session
     */
    public record IssuedRefreshToken(String token, String jti, Instant expiresAt) {
    }

    /**
     * Emet un refresh token unique. Le {@code jti} rend deux jetons emis dans la meme seconde
     * pour le meme compte distinguables, ce qui permet a plusieurs appareils d'avoir chacun
     * leur session revocable independamment.
     */
    public IssuedRefreshToken generateRefreshToken(String subject) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshExpirationMs);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(subject)
                .id(jti)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new IssuedRefreshToken(token, jti, expiresAt);
    }

    /**
     * Vérifie la signature et l'expiration, et contrôle que le jeton est bien du type attendu.
     *
     * @return le sujet (email de l'utilisateur), ou {@code null} si le jeton est invalide.
     */
    public String extractSubject(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sujet et {@code jti} d'un refresh token vérifié, ou {@code null} s'il est invalide.
     *
     * <p>La signature et l'expiration sont contrôlées ici ; la révocation, elle, se vérifie en
     * base à partir du {@code jti} — un jeton cryptographiquement valide peut avoir été révoqué.
     */
    public RefreshClaims parseRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
                return null;
            }
            return new RefreshClaims(claims.getSubject(), claims.getId());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public record RefreshClaims(String subject, String jti) {
    }

    /** Durée de vie de l'access token, en secondes — exposée au frontend. */
    public long getAccessExpirationSeconds() {
        return accessExpirationMs / 1000;
    }

    /** Durée de vie du refresh token, en secondes — utilisée pour le Max-Age du cookie. */
    public long getRefreshExpirationSeconds() {
        return refreshExpirationMs / 1000;
    }
}

package com.raikiservices.backend.dto.session;

import java.time.Instant;

import com.raikiservices.backend.entity.RefreshToken;
import com.raikiservices.backend.entity.User;

/**
 * Session active telle qu'affichée dans le back-office.
 *
 * @param current vrai pour la session depuis laquelle la requête est faite — le frontend
 *                peut ainsi éviter de proposer « révoquer » sur la connexion en cours
 */
public record SessionResponse(
        Long id,
        Long userId,
        String username,
        String userEmail,
        String userStatus,
        String device,
        String ipAddress,
        Instant createdAt,
        Instant lastUsedAt,
        Instant expiresAt,
        boolean current) {

    public static SessionResponse from(RefreshToken session, String currentJti) {
        User user = session.getUser();
        return new SessionResponse(
                session.getId(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus() == null ? null : user.getStatus().name(),
                session.getDevice(),
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiryDate(),
                session.getJti().equals(currentJti));
    }
}

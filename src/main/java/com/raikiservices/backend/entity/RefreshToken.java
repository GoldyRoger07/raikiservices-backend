package com.raikiservices.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Data;

/**
 * Session ouverte : une ligne par refresh token en circulation.
 *
 * <p>Contrairement au backend Secogroupe, où la relation est {@code @OneToOne} et où chaque
 * connexion écrase la précédente, la relation est ici {@code @ManyToOne} : un même compte peut
 * être connecté sur plusieurs appareils, et chacun se révoque séparément.
 *
 * <p>Seul le {@code jti} du jeton est stocké, jamais le jeton lui-même : une fuite de la base
 * ne livre donc aucun identifiant de connexion réutilisable.
 */
@Entity
@Data
public class RefreshToken {

    @Id
    @GeneratedValue
    private Long id;

    /** Identifiant unique porté par le JWT de refresh (claim {@code jti}). */
    @Column(nullable = false, unique = true)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** User-Agent tronqué, affiché dans la liste des sessions. */
    @Column(length = 400)
    private String device;

    private String ipAddress;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(updatable = false)
    private Instant createdAt;

    /** Mis à jour à chaque rafraîchissement : trahit une session inactive. */
    private Instant lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUsedAt = createdAt;
    }
}

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
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Abonnement Web Push d'un navigateur. Un compte peut en avoir plusieurs, un par appareil.
 *
 * <p>Pas de contrainte d'unicité SQL sur endpoint : l'URL dépasse la taille indexable d'une
 * clé MySQL. La déduplication se fait en code, au moment de la souscription.
 */
@Entity
@Data
@Table(name = "push_subscriptions")
public class PushSubscription {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1024)
    private String endpoint;

    /** Clé publique du client, en base64url. */
    @Column(nullable = false)
    private String p256dh;

    /** Secret d'authentification du client, en base64url. */
    @Column(nullable = false)
    private String auth;

    @Column(length = 400)
    private String userAgent;

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

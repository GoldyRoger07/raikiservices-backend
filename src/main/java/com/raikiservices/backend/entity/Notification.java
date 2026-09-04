package com.raikiservices.backend.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Notification archivée, avec son état lu/non-lu.
 *
 * <p>Destinataire et lecteurs sont désignés par identifiant de compte plutôt que par nom
 * d'utilisateur : un compte renommé ne perd donc pas son historique.
 */
@Entity
@Data
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipientId"),
        @Index(name = "idx_notification_created_at", columnList = "createdAt")
})
public class Notification {

    @Id
    @GeneratedValue
    private Long id;

    /** Code de l'évènement, cf. NotificationType.getCode(). */
    private String type;

    private String title;

    @Column(length = 1000)
    private String message;

    /** Identifiant de l'objet concerné : message de contact, article... */
    private Long entityId;

    /** Destinataire ; null pour une notification visible de tous. */
    private Long recipientId;

    /**
     * Comptes ayant marqué la notification comme lue. Un ensemble, et non un booléen, pour que
     * l'état lu reste individuel sur une notification diffusée à plusieurs personnes.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "notification_read_by", joinColumns = @JoinColumn(name = "notification_id"))
    @Column(name = "user_id")
    private Set<Long> readBy = new HashSet<>();

    @Column(updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

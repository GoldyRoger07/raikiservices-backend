package com.raikiservices.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * Choix de canaux d'un utilisateur pour un type d'évènement : une ligne par couple
 * (compte, type). Absente, elle vaut les valeurs par défaut ci-dessous.
 */
@Entity
@Data
@Table(name = "notification_preferences", uniqueConstraints = @UniqueConstraint(
        name = "uk_notif_pref_user_event", columnNames = { "user_id", "event_type" }))
public class NotificationPreference {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Cloche du back-office et temps réel. */
    @Column(nullable = false)
    private boolean inApp = true;

    @Column(nullable = false)
    private boolean email = true;

    /** Désactivé par défaut : le push exige d'abord l'accord explicite du navigateur. */
    @Column(nullable = false)
    private boolean push = false;
}

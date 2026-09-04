package com.raikiservices.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Interrupteur global par type d'évènement. À false, l'action correspondante n'émet plus rien
 * du tout : ni archive, ni temps réel, ni email, ni push.
 */
@Entity
@Data
@Table(name = "notification_settings")
public class NotificationSetting {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventType;

    private String label;

    @Column(nullable = false)
    private boolean enabled = true;

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

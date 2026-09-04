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
 * Trace de tentative de connexion, réussie ou non.
 *
 * <p>Les échecs sont enregistrés aussi : une rafale d'échecs sur un compte est le signal
 * qu'une attaque par force brute est en cours.
 */
@Entity
@Data
public class LoginHistory {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(updatable = false)
    private Instant date;

    @Column(length = 400)
    private String device;

    private String ipAddress;

    private boolean success;

    @PrePersist
    protected void onCreate() {
        if (date == null) {
            date = Instant.now();
        }
    }
}

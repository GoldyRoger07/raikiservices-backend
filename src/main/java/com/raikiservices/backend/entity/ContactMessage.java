package com.raikiservices.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import lombok.Data;

/**
 * Message déposé via le formulaire de contact du site.
 *
 * <p>Transposé de {@code QuoteRequest} du backend Secogroupe, avec un champ {@code message} :
 * un formulaire de contact d'agence recueille une demande rédigée, là où le formulaire de
 * devis Secogroupe ne collectait que des coordonnées.
 */
@Entity
@Data
public class ContactMessage {

    @Id
    @GeneratedValue
    private Long id;

    private String firstName;
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    /** Société ou organisation du contact, le cas échéant. */
    private String companyName;

    /** Prestation visée : site vitrine, e-commerce, refonte… */
    private String serviceCategory;

    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    private boolean newsletterOptIn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactStatus status = ContactStatus.NEW;

    /** Notes internes de suivi, jamais exposées côté public. */
    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }
}

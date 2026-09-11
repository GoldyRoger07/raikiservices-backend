package com.raikiservices.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import lombok.Data;

/**
 * Offre de lancement : création offerte pour les premiers clients.
 *
 * <p>Une seule ligne en base, créée au premier accès. C'est la seule donnée commerciale que
 * le site public lise ailleurs que dans ses fichiers de configuration : le nombre de places
 * restantes doit pouvoir changer entre deux déploiements, puisque c'est lui qui décide si
 * l'offre s'affiche encore.
 *
 * <p>Le texte de l'offre, lui, n'est pas ici — il vit dans les fichiers de contenu du
 * frontend, comme le reste de la page. Ici on ne garde que ce qui bouge.
 */
@Entity
@Data
public class LaunchOffer {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * Interrupteur général. Le mettre à faux retire l'offre du site sans toucher au
     * compteur — pratique pour la suspendre le temps d'absorber les demandes en cours.
     */
    @Column(nullable = false)
    private boolean active = true;

    /** Nombre de places ouvertes au total. */
    @Column(nullable = false)
    private int totalSlots = 10;

    /** Places déjà attribuées. L'offre disparaît d'elle-même quand elle rejoint le total. */
    @Column(nullable = false)
    private int claimedSlots = 0;

    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Places encore disponibles, jamais négatif. */
    public int remainingSlots() {
        return Math.max(0, totalSlots - claimedSlots);
    }

    /** L'offre est-elle réellement visible du public ? */
    public boolean isRunning() {
        return active && remainingSlots() > 0;
    }
}

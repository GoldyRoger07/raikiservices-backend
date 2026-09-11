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
 * Fiche locale d'une image hébergée chez Cloudinary.
 *
 * <p>Le fichier vit chez Cloudinary, son inventaire ici. Tenir cette table plutôt que
 * d'interroger l'API d'administration de Cloudinary à chaque affichage évite d'en consommer
 * le quota — limité, et distinct de celui de livraison — et rend la bibliothèque du
 * back-office paginable comme les autres listes, sans dépendre de la disponibilité d'un
 * service tiers. Cloudinary n'est sollicité qu'à l'envoi et à la suppression.
 */
@Entity
@Data
public class MediaAsset {

    @Id
    @GeneratedValue
    private Long id;

    /** Identifiant Cloudinary, dossier compris — « raiki/projets/abc123 ». */
    @Column(nullable = false, unique = true)
    private String publicId;

    /** Adresse livrée par Cloudinary à l'envoi, sans transformation. */
    @Column(nullable = false, length = 1000)
    private String secureUrl;

    private String format;

    private Integer width;

    private Integer height;

    private Long bytes;

    /** Dossier Cloudinary, pour regrouper la bibliothèque par usage. */
    private String folder;

    private String originalFilename;

    /**
     * Texte alternatif, repris tel quel dans l'attribut {@code alt} des images du site.
     * Renseigné à l'envoi puis modifiable : une vitrine sans texte alternatif est
     * inaccessible aux lecteurs d'écran et muette pour les moteurs de recherche.
     */
    @Column(length = 500)
    private String alt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(updatable = false)
    private Instant uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = Instant.now();
    }
}

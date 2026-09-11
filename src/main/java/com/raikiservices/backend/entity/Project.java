package com.raikiservices.backend.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

/**
 * Réalisation présentée sur le site vitrine.
 *
 * <p>Une seule entité alimente les trois emplacements du site, départagés par trois
 * drapeaux : tout projet publié figure dans le portfolio ; {@code caseStudy} le fait
 * apparaître en plus sur la page des études de cas, où {@code description} et
 * {@code services} sont mis en avant ; {@code featured} le remonte sur l'accueil. Un client
 * n'est ainsi saisi qu'une fois, quel que soit le nombre de pages qui le montrent.
 */
@Entity
@Data
public class Project {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    /** Qualification du client, affichée sous le titre — « Restaurant — Pétion-Ville ». */
    private String clientLabel;

    /** Une phrase, pour la carte du portfolio et de l'accueil. */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** Corps de l'étude de cas. Absent des listes, comme le contenu d'un article. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Classe d'icône PrimeIcons affichée sur l'étude de cas — « pi pi-desktop ». */
    private String icon;

    /** Site livré, cible du lien « Voir le site ». */
    private String websiteUrl;

    /**
     * Identifiant Cloudinary de l'image de couverture, jamais une URL complète : l'adresse
     * est recomposée à l'affichage avec ses transformations, ce qui permet d'en changer
     * sans toucher aux données.
     */
    private String coverPublicId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_gallery", joinColumns = @JoinColumn(name = "project_id"))
    @OrderColumn(name = "position")
    @Column(name = "public_id")
    private List<String> galleryPublicIds = new ArrayList<>();

    /** Prestations réalisées — « Web Design », « SEO »… */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_services", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "service")
    private Set<String> services = new HashSet<>();

    /** Secteur d'activité du client, pour filtrer la vitrine plus tard. */
    private String sector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.DRAFT;

    /** Le projet figure sur la page des études de cas. */
    @Column(nullable = false)
    private boolean caseStudy = false;

    /** Le projet est mis en avant sur la page d'accueil. */
    @Column(nullable = false)
    private boolean featured = false;

    /**
     * Rang d'affichage sur le site public, croissant.
     *
     * <p>C'est ce qui distingue une vitrine d'un blog : les réalisations se rangent à la
     * main, pas par date. Les projets partageant un même rang sont départagés par leur date
     * de publication, la plus récente d'abord.
     */
    @Column(nullable = false)
    private int displayOrder = 0;

    /** Renseignée au passage en PUBLISHED. */
    private LocalDateTime publishedAt;

    @Column(updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

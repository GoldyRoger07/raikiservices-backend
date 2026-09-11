package com.raikiservices.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.Project;
import com.raikiservices.backend.entity.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySlug(String slug);

    Optional<Project> findBySlugAndStatus(String slug, ProjectStatus status);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    /** Réalisations détaillées, pour la page des études de cas. */
    Page<Project> findByStatusAndCaseStudyTrue(ProjectStatus status, Pageable pageable);

    /** Réalisations mises en avant sur l'accueil. */
    Page<Project> findByStatusAndFeaturedTrue(ProjectStatus status, Pageable pageable);

    @Query("""
            SELECT p FROM Project p WHERE
            LOWER(p.title)       LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.slug)        LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.clientLabel) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.summary)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.sector)      LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<Project> search(@Param("f") String filter, Pageable pageable);

    @Query("""
            SELECT p FROM Project p WHERE p.status = :status AND (
            LOWER(p.title)       LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.clientLabel) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.summary)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.sector)      LIKE LOWER(CONCAT('%', :f, '%')))
            """)
    Page<Project> searchByStatus(@Param("f") String filter,
            @Param("status") ProjectStatus status,
            Pageable pageable);

    /**
     * Titres des projets qui affichent cette image, en couverture ou dans leur galerie.
     *
     * <p>Interrogé avant toute suppression d'image : sans ce garde-fou, retirer un fichier
     * de la bibliothèque laisserait des emplacements vides sur le site public, que
     * personne ne remarquerait avant la prochaine visite de la page concernée.
     */
    @Query("""
            SELECT DISTINCT p.title FROM Project p
            LEFT JOIN p.galleryPublicIds g
            WHERE p.coverPublicId = :publicId OR g = :publicId
            """)
    List<String> findTitlesUsingImage(@Param("publicId") String publicId);

    /**
     * Rang le plus élevé déjà attribué, {@code -1} si la table est vide : un projet créé
     * prend le rang suivant et se range donc en fin de vitrine plutôt que de s'insérer
     * devant les réalisations déjà ordonnées.
     */
    @Query("SELECT COALESCE(MAX(p.displayOrder), -1) FROM Project p")
    int findMaxDisplayOrder();
}

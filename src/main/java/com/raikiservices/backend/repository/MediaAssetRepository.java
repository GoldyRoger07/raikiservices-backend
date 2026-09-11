package com.raikiservices.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.MediaAsset;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Optional<MediaAsset> findByPublicId(String publicId);

    /** Contenu d'un dossier et de ses sous-dossiers. */
    Page<MediaAsset> findByFolderStartingWith(String folder, Pageable pageable);

    @Query("""
            SELECT m FROM MediaAsset m WHERE
            LOWER(m.publicId)         LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(m.originalFilename) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(m.alt)              LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<MediaAsset> search(@Param("f") String filter, Pageable pageable);

    @Query("""
            SELECT m FROM MediaAsset m WHERE m.folder LIKE CONCAT(:folder, '%') AND (
            LOWER(m.publicId)         LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(m.originalFilename) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(m.alt)              LIKE LOWER(CONCAT('%', :f, '%')))
            """)
    Page<MediaAsset> searchInFolder(@Param("f") String filter,
            @Param("folder") String folder,
            Pageable pageable);
}

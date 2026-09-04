package com.raikiservices.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.BlogPost;
import com.raikiservices.backend.entity.BlogStatus;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);

    Optional<BlogPost> findBySlugAndStatus(String slug, BlogStatus status);

    @Query("""
            SELECT b FROM BlogPost b WHERE
            LOWER(b.title)    LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(b.slug)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(b.excerpt)  LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(b.category) LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<BlogPost> search(@Param("f") String filter, Pageable pageable);

    @Query("""
            SELECT b FROM BlogPost b WHERE b.status = :status AND (
            LOWER(b.title)    LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(b.excerpt)  LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(b.category) LIKE LOWER(CONCAT('%', :f, '%')))
            """)
    Page<BlogPost> searchByStatus(@Param("f") String filter,
            @Param("status") BlogStatus status,
            Pageable pageable);
}

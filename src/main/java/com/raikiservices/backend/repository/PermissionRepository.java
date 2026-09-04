package com.raikiservices.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    @Query("""
            SELECT p FROM Permission p WHERE
            LOWER(p.name)        LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.module)      LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.action)      LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(p.description) LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<Permission> search(@Param("f") String filter, Pageable pageable);
}

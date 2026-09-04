package com.raikiservices.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.Permission;
import com.raikiservices.backend.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    /** Rôles portant une permission donnée — bloque la suppression d'une permission en usage. */
    List<Role> findByPermissionsContaining(Permission permission);

    @Query("""
            SELECT r FROM Role r WHERE
            LOWER(r.name)        LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(r.description) LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<Role> search(@Param("f") String filter, Pageable pageable);
}

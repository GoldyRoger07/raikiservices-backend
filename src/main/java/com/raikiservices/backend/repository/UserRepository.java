package com.raikiservices.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.Role;
import com.raikiservices.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /** Comptes portant un rôle donné — bloque la suppression d'un rôle encore attribué. */
    long countByRolesContaining(Role role);

    /** Connexion par email ou par nom d'utilisateur (un seul identifiant côté formulaire). */
    @Query("select u from User u where u.email = :login or u.username = :login")
    Optional<User> findByEmailOrUsername(@Param("login") String login);

    /**
     * Comptes actifs détenant une permission donnée, via l'un de leurs rôles.
     *
     * <p>Sert à cibler les destinataires d'une notification : seuls ceux qui ont le droit de
     * consulter la donnée concernée sont prévenus. Les comptes désactivés sont écartés — ils
     * ne peuvent de toute façon pas se connecter pour lire la notification.
     *
     * <p>Sert aussi de garde-fou anti-verrouillage : avant de retirer les droits d'un compte,
     * on vérifie qu'il en reste un autre capable d'administrer les utilisateurs.
     */
    @Query("""
            select distinct u from User u
            join u.roles r
            join r.permissions p
            where p.name = :permission and u.enabled = true
            """)
    List<User> findByPermission(@Param("permission") String permission);

    @Query("""
            SELECT u FROM User u WHERE
            LOWER(u.username)  LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(u.email)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(u.phone)     LIKE LOWER(CONCAT('%', :f, '%')) OR
            LOWER(u.position)  LIKE LOWER(CONCAT('%', :f, '%'))
            """)
    Page<User> search(@Param("f") String filter, Pageable pageable);
}

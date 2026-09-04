package com.raikiservices.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    /** Connexion par email ou par nom d'utilisateur (un seul identifiant côté formulaire). */
    @Query("select u from User u where u.email = :login or u.username = :login")
    Optional<User> findByEmailOrUsername(@Param("login") String login);

    /**
     * Comptes actifs détenant une permission donnée, via l'un de leurs rôles.
     *
     * <p>Sert à cibler les destinataires d'une notification : seuls ceux qui ont le droit de
     * consulter la donnée concernée sont prévenus. Les comptes désactivés sont écartés — ils
     * ne peuvent de toute façon pas se connecter pour lire la notification.
     */
    @Query("""
            select distinct u from User u
            join u.roles r
            join r.permissions p
            where p.name = :permission and u.enabled = true
            """)
    List<User> findByPermission(@Param("permission") String permission);
}

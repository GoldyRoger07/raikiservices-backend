package com.raikiservices.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.raikiservices.backend.entity.RefreshToken;
import com.raikiservices.backend.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    @EntityGraph(attributePaths = "user")
    List<RefreshToken> findByUserOrderByLastUsedAtDesc(User user);

    @EntityGraph(attributePaths = "user")
    List<RefreshToken> findAllByOrderByLastUsedAtDesc();

    void deleteByUser(User user);

    void deleteByJti(String jti);

    /** Purge les sessions expirées : sans elle, la table ne cesserait de croître. */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    int deleteExpired(@Param("now") Instant now);
}

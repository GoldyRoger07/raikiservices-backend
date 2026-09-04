package com.raikiservices.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.raikiservices.backend.entity.EmailVerificationToken;
import com.raikiservices.backend.entity.User;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByLinkToken(String linkToken);

    Optional<EmailVerificationToken> findByUser(User user);

    void deleteByUser(User user);
}

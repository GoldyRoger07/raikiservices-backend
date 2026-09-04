package com.raikiservices.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.session.SessionResponse;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.RefreshTokenRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Consultation et révocation des sessions ouvertes.
 *
 * <p>Deux portées coexistent : un utilisateur gère ses propres sessions (« déconnecter mes
 * autres appareils »), un administrateur porteur de {@code READ_SESSION} / {@code DELETE_SESSION}
 * voit et coupe celles de tout le monde.
 */
@Service
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public SessionService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /** Toutes les sessions, tous comptes confondus — vue d'administration. */
    @Transactional(readOnly = true)
    public List<SessionResponse> getAll(String currentJti) {
        return refreshTokenRepository.findAllByOrderByLastUsedAtDesc().stream()
                .map(session -> SessionResponse.from(session, currentJti))
                .toList();
    }

    /** Sessions d'un compte donné. */
    @Transactional(readOnly = true)
    public List<SessionResponse> getForUser(Long userId, String currentJti) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", userId));

        return refreshTokenRepository.findByUserOrderByLastUsedAtDesc(user).stream()
                .map(session -> SessionResponse.from(session, currentJti))
                .toList();
    }

    @Transactional
    public void revokeById(Long id) {
        if (!refreshTokenRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Session", id);
        }
        refreshTokenRepository.deleteById(id);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Utilisateur", userId));
        refreshTokenRepository.deleteByUser(user);
    }
}

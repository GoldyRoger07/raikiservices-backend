package com.raikiservices.backend.service;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.entity.RefreshToken;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.repository.RefreshTokenRepository;
import com.raikiservices.backend.security.JwtService;
import com.raikiservices.backend.security.JwtService.IssuedRefreshToken;

/**
 * Cycle de vie des sessions : ouverture à la connexion, rotation au rafraîchissement,
 * fermeture à la déconnexion ou à la révocation.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int DEVICE_MAX_LENGTH = 400;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    /**
     * Jeton a deposer dans le cookie, et identifiant de la session correspondante.
     *
     * @param jti a placer dans l'access token pour que les autres endpoints sachent de quelle
     *            session provient la requete
     */
    public record OpenedSession(String refreshToken, String jti) {
    }

    /** Ouvre une session et renvoie le jeton signé à déposer dans le cookie. */
    @Transactional
    public OpenedSession openSession(User user, String device, String ipAddress) {
        IssuedRefreshToken issued = jwtService.generateRefreshToken(user.getEmail());

        RefreshToken session = new RefreshToken();
        session.setJti(issued.jti());
        session.setUser(user);
        session.setDevice(truncate(device));
        session.setIpAddress(ipAddress);
        session.setExpiryDate(issued.expiresAt());
        refreshTokenRepository.save(session);

        return new OpenedSession(issued.token(), issued.jti());
    }

    /**
     * Vérifie que le {@code jti} correspond à une session vivante.
     *
     * <p>Une session expirée est supprimée au passage plutôt que laissée en base.
     *
     * @return la session, ou {@link Optional#empty()} si elle est révoquée ou expirée
     */
    @Transactional
    public Optional<RefreshToken> findLiveSession(String jti) {
        Optional<RefreshToken> found = refreshTokenRepository.findByJti(jti);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken session = found.get();
        if (session.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(session);
            return Optional.empty();
        }
        return found;
    }

    /**
     * Remplace une session par une nouvelle, en conservant appareil et IP d'origine.
     *
     * <p>La rotation ferme l'ancienne session : un refresh token intercepté ne sert qu'une
     * fois, et sa réutilisation échoue puisque le {@code jti} n'existe plus.
     */
    @Transactional
    public OpenedSession rotate(RefreshToken currentSession) {
        User user = currentSession.getUser();
        String device = currentSession.getDevice();
        String ip = currentSession.getIpAddress();

        refreshTokenRepository.delete(currentSession);
        return openSession(user, device, ip);
    }

    /** Ferme la session portée par ce {@code jti}, sans erreur si elle a déjà disparu. */
    @Transactional
    public void closeSession(String jti) {
        refreshTokenRepository.deleteByJti(jti);
    }

    @Transactional
    public void closeAllSessions(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Élimine chaque nuit les sessions dont la date d'expiration est passée. Sans cette purge,
     * la table conserverait indéfiniment les sessions des visiteurs qui ne se déconnectent pas.
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void purgeExpiredSessions() {
        int removed = refreshTokenRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purge des sessions expirées : {} supprimée(s).", removed);
        }
    }

    private String truncate(String device) {
        if (device == null) {
            return null;
        }
        return device.length() <= DEVICE_MAX_LENGTH ? device : device.substring(0, DEVICE_MAX_LENGTH);
    }
}

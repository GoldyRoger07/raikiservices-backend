package com.raikiservices.backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.auth.AuthResponse;
import com.raikiservices.backend.dto.auth.LoginRequest;
import com.raikiservices.backend.dto.auth.RegisterRequest;
import com.raikiservices.backend.dto.auth.ResetPasswordRequest;
import com.raikiservices.backend.dto.auth.UserSummaryResponse;
import com.raikiservices.backend.dto.notification.SseEvent;
import com.raikiservices.backend.entity.EmailVerificationToken;
import com.raikiservices.backend.entity.PasswordResetToken;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.entity.RefreshToken;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.entity.UserStatus;
import com.raikiservices.backend.exception.BusinessRuleException;
import com.raikiservices.backend.repository.EmailVerificationTokenRepository;
import com.raikiservices.backend.repository.PasswordResetTokenRepository;
import com.raikiservices.backend.repository.UserRepository;
import com.raikiservices.backend.security.CustomUserDetailsService;
import com.raikiservices.backend.security.JwtService;
import com.raikiservices.backend.security.JwtService.RefreshClaims;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.RefreshTokenService.OpenedSession;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final long OTP_TTL_MINUTES = 15;
    private static final long RESET_TTL_MINUTES = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginHistoryService loginHistoryService;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public AuthService(AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginHistoryService loginHistoryService,
            UserRepository userRepository,
            EmailVerificationTokenRepository verificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            NotificationService notificationService) {

        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginHistoryService = loginHistoryService;
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    /**
     * Paire de jetons issue d'une connexion ou d'un rafraîchissement.
     *
     * <p>Le refresh token est séparé de la réponse HTTP : le contrôleur le place dans un
     * cookie plutôt que dans le corps JSON.
     */
    public record AuthTokens(AuthResponse response, String refreshToken) {
    }

    // ──────────────── Connexion ────────────────

    @Transactional
    public AuthTokens login(LoginRequest request, String device, String ipAddress) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.password()));
        } catch (AuthenticationException e) {
            loginHistoryService.record(request.login(), ipAddress, device, false);
            throw e;
        }

        loginHistoryService.record(request.login(), ipAddress, device, true);

        SecurityUser principal = (SecurityUser) authentication.getPrincipal();
        User user = principal.getUser();
        OpenedSession session = refreshTokenService.openSession(user, device, ipAddress);
        return new AuthTokens(buildResponse(user, session.jti()), session.refreshToken());
    }

    /**
     * Échange un refresh token contre un nouvel access token.
     *
     * <p>Deux contrôles indépendants : la signature du JWT, puis l'existence de la session en
     * base. Le second est ce qui rend une session révocable — un jeton parfaitement signé mais
     * dont la session a été coupée est refusé.
     */
    @Transactional
    public AuthTokens refresh(String refreshToken) {
        RefreshClaims claims = parseOrReject(refreshToken);

        RefreshToken session = refreshTokenService.findLiveSession(claims.jti())
                .orElseThrow(() -> new BadCredentialsException("Session expirée ou révoquée."));

        SecurityUser principal = (SecurityUser) userDetailsService.loadUserByUsername(claims.subject());
        if (!principal.isEnabled()) {
            refreshTokenService.closeSession(claims.jti());
            throw new BadCredentialsException("Compte désactivé.");
        }

        OpenedSession rotated = refreshTokenService.rotate(session);
        return new AuthTokens(buildResponse(principal.getUser(), rotated.jti()), rotated.refreshToken());
    }

    /** Ferme la session portée par le cookie. Un cookie absent ou périmé n'est pas une erreur. */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        RefreshClaims claims = jwtService.parseRefreshToken(refreshToken);
        if (claims != null) {
            refreshTokenService.closeSession(claims.jti());
        }
    }

    // ──────────────── Inscription et vérification d'email ────────────────

    /**
     * Crée un compte désactivé et envoie le code de vérification.
     *
     * <p>Le compte ne reçoit aucun rôle : un nouvel inscrit s'authentifie mais n'accède à rien
     * tant qu'un administrateur ne lui en attribue pas.
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessRuleException("Un compte existe déjà avec cette adresse email.");
        }
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessRuleException("Ce nom d'utilisateur est déjà pris.");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(false);
        user.setStatus(UserStatus.INACTIVE);
        user.setRoles(Set.of());
        User saved = userRepository.save(user);

        issueVerification(saved);

        notificationService.dispatch(new SseEvent(
                NotificationType.NEW_USER_REGISTERED.getCode(),
                "Nouvelle inscription",
                saved.getUsername() + " (" + saved.getEmail() + ")",
                saved.getId()));
    }

    /** Valide le compte à partir du code à 6 chiffres saisi dans le formulaire. */
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("Code invalide ou expiré."));

        EmailVerificationToken token = verificationTokenRepository.findByUser(user)
                .orElseThrow(() -> new BusinessRuleException("Code invalide ou expiré."));

        if (!token.getOtpCode().equals(otpCode) || token.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessRuleException("Code invalide ou expiré.");
        }

        activate(user, token);
    }

    /** Valide le compte à partir du lien cliquable reçu par email. */
    @Transactional
    public void verifyEmailByLink(String linkToken) {
        EmailVerificationToken token = verificationTokenRepository.findByLinkToken(linkToken)
                .orElseThrow(() -> new BusinessRuleException("Lien invalide ou déjà utilisé."));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.delete(token);
            throw new BusinessRuleException("Lien expiré. Demandez un nouvel email de vérification.");
        }

        activate(token.getUser(), token);
    }

    /**
     * Renvoie un code de vérification.
     *
     * <p>Ne signale ni compte inconnu ni compte déjà validé : la réponse est identique dans
     * tous les cas, sans quoi l'endpoint permettrait de tester quelles adresses sont inscrites.
     */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email)
                .filter(user -> !user.isEnabled())
                .ifPresent(this::issueVerification);
    }

    // ──────────────── Mot de passe oublié ────────────────

    /** Envoie un lien de réinitialisation. Silencieux si l'adresse est inconnue. */
    @Transactional
    public void forgotPassword(String email) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            log.info("Demande de réinitialisation pour une adresse inconnue — aucune action.");
            return;
        }

        User user = found.get();
        passwordResetTokenRepository.deleteByUser(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(Instant.now().plus(RESET_TTL_MINUTES, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token.getToken());
    }

    /**
     * Applique le nouveau mot de passe et ferme toutes les sessions du compte.
     *
     * <p>Fermer les sessions est le point essentiel : si le mot de passe est réinitialisé parce
     * qu'il avait fuité, laisser vivre les sessions ouvertes laisserait l'intrus connecté.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessRuleException("Lien invalide ou déjà utilisé."));

        if (token.getExpiryDate().isBefore(Instant.now())) {
            passwordResetTokenRepository.delete(token);
            throw new BusinessRuleException("Lien expiré. Refaites une demande.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        passwordResetTokenRepository.delete(token);
        refreshTokenService.closeAllSessions(user);
    }

    // ──────────────── Interne ────────────────

    private RefreshClaims parseOrReject(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token absent.");
        }
        RefreshClaims claims = jwtService.parseRefreshToken(refreshToken);
        if (claims == null || claims.jti() == null) {
            throw new BadCredentialsException("Refresh token invalide ou expiré.");
        }
        return claims;
    }

    private AuthResponse buildResponse(User user, String sessionJti) {
        return AuthResponse.of(
                jwtService.generateAccessToken(user.getEmail(), sessionJti),
                jwtService.getAccessExpirationSeconds(),
                UserSummaryResponse.from(user));
    }

    /** Remplace tout code en cours par un nouveau, puis l'envoie. */
    private void issueVerification(User user) {
        verificationTokenRepository.deleteByUser(user);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .otpCode(String.format("%06d", RANDOM.nextInt(1_000_000)))
                .linkToken(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES))
                .build();
        verificationTokenRepository.save(token);

        emailService.sendVerificationEmail(
                user.getEmail(), user.getUsername(), token.getOtpCode(), token.getLinkToken());
    }

    private void activate(User user, EmailVerificationToken token) {
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        verificationTokenRepository.delete(token);
    }
}

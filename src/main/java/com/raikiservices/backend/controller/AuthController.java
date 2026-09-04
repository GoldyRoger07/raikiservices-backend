package com.raikiservices.backend.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.auth.AuthResponse;
import com.raikiservices.backend.dto.auth.EmailRequest;
import com.raikiservices.backend.dto.auth.LoginRequest;
import com.raikiservices.backend.dto.auth.MessageResponse;
import com.raikiservices.backend.dto.auth.RegisterRequest;
import com.raikiservices.backend.dto.auth.ResetPasswordRequest;
import com.raikiservices.backend.dto.auth.UserSummaryResponse;
import com.raikiservices.backend.dto.auth.VerifyOtpRequest;
import com.raikiservices.backend.security.RefreshTokenCookieFactory;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.AuthService;
import com.raikiservices.backend.service.AuthService.AuthTokens;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshTokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    // ──────────────── Inscription et vérification ────────────────

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.accepted().body(new MessageResponse(
                "Compte créé. Un code de vérification vient de vous être envoyé par email."));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.email(), request.otpCode());
        return ResponseEntity.ok(new MessageResponse("Adresse email vérifiée. Vous pouvez vous connecter."));
    }

    /** Cible du lien cliquable envoyé par email : ouvert dans un navigateur, donc en GET. */
    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        authService.verifyEmailByLink(token);
        return ResponseEntity.ok(new MessageResponse("Adresse email vérifiée. Vous pouvez vous connecter."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        authService.resendVerification(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(
                "Si un compte non vérifié existe pour cette adresse, un nouveau code vient d'être envoyé."));
    }

    // ──────────────── Mot de passe oublié ────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.accepted().body(new MessageResponse(
                "Si un compte existe pour cette adresse, un lien de réinitialisation vient d'être envoyé."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .body(new MessageResponse(
                        "Mot de passe mis à jour. Toutes les sessions ouvertes ont été fermées."));
    }

    // ──────────────── Connexion ────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        return withRefreshCookie(authService.login(request, device(httpRequest), clientIp(httpRequest)));
    }

    /** Le refresh token est lu dans le cookie : rien à envoyer dans le corps de la requête. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        return withRefreshCookie(authService.refresh(cookieFactory.read(request)));
    }

    /**
     * Ferme la session côté serveur et efface le cookie. L'access token déjà émis reste valide
     * jusqu'à son expiration (quelques minutes) : il n'y a pas de liste de révocation.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(cookieFactory.read(request));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    /** Profil de l'utilisateur connecté — sert au frontend à réhydrater sa session. */
    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> me(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(UserSummaryResponse.from(principal.getUser()));
    }

    // ──────────────── Interne ────────────────

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthTokens tokens) {
        ResponseCookie cookie = cookieFactory.create(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(tokens.response());
    }

    private String device(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    /**
     * Adresse du client, en tenant compte d'un éventuel reverse proxy.
     *
     * <p>{@code X-Forwarded-For} est déclaratif : il ne fait foi que si un proxy de confiance
     * le réécrit devant l'application. Utilisé ici pour l'affichage des sessions, jamais comme
     * élément de décision de sécurité.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

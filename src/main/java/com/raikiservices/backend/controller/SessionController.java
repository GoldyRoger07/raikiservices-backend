package com.raikiservices.backend.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.session.SessionResponse;
import com.raikiservices.backend.security.JwtService;
import com.raikiservices.backend.security.RefreshTokenCookieFactory;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.SessionService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final RefreshTokenCookieFactory cookieFactory;
    private final JwtService jwtService;

    public SessionController(SessionService sessionService,
            RefreshTokenCookieFactory cookieFactory,
            JwtService jwtService) {

        this.sessionService = sessionService;
        this.cookieFactory = cookieFactory;
        this.jwtService = jwtService;
    }

    /** Toutes les sessions ouvertes, tous comptes confondus. */
    @PreAuthorize("hasAuthority('READ_SESSION')")
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getAll(HttpServletRequest request) {
        return ResponseEntity.ok(sessionService.getAll(currentJti(request)));
    }

    /** Ses propres sessions — accessible à tout compte connecté, sans permission dédiée. */
    @GetMapping("/me")
    public ResponseEntity<List<SessionResponse>> getMine(@AuthenticationPrincipal SecurityUser principal,
            HttpServletRequest request) {

        return ResponseEntity.ok(sessionService.getForUser(principal.getId(), currentJti(request)));
    }

    /**
     * Ferme toutes ses propres sessions, y compris celle en cours.
     *
     * <p>C'est le geste à faire après un vol d'appareil : il n'exige aucune permission
     * particulière puisqu'il ne porte que sur le compte de l'appelant.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> revokeMine(@AuthenticationPrincipal SecurityUser principal) {
        sessionService.revokeAllForUser(principal.getId());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    @PreAuthorize("hasAuthority('DELETE_SESSION')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeById(@PathVariable Long id) {
        sessionService.revokeById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('DELETE_SESSION')")
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> revokeAllForUser(@PathVariable Long userId) {
        sessionService.revokeAllForUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Identifie la session courante pour que le frontend puisse la distinguer dans la liste.
     *
     * <p>L'information est lue dans le claim {@code sid} de l'access token, et non dans le
     * cookie de refresh : celui-ci est limite au chemin {@code /api/v1/auth} et n'accompagne
     * donc jamais un appel a cet endpoint.
     */
    private String currentJti(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return jwtService.extractSessionId(header.substring("Bearer ".length()).trim());
    }
}

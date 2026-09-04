package com.raikiservices.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.notification.NotificationPreferenceResponse;
import com.raikiservices.backend.dto.notification.NotificationPreferenceUpdateRequest;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.NotificationPreferenceService;

import jakarta.validation.Valid;

/**
 * Préférences de canal du compte connecté. Aucune permission requise : chacun règle les
 * siennes, et personne ne touche à celles des autres.
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferencesController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferencesController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> getMine(
            @AuthenticationPrincipal SecurityUser principal) {

        return ResponseEntity.ok(preferenceService.getForUser(principal.getUser()));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceResponse> update(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody NotificationPreferenceUpdateRequest request) {

        return ResponseEntity.ok(preferenceService.update(principal.getUser(), request));
    }
}

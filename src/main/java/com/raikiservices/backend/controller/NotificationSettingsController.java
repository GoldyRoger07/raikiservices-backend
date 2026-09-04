package com.raikiservices.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.notification.NotificationSettingResponse;
import com.raikiservices.backend.dto.notification.NotificationSettingUpdateRequest;
import com.raikiservices.backend.service.NotificationSettingsService;

import jakarta.validation.Valid;

/** Interrupteurs globaux : réservés aux comptes habilités à configurer les notifications. */
@RestController
@RequestMapping("/api/v1/notification-settings")
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    public NotificationSettingsController(NotificationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @PreAuthorize("hasAuthority('READ_NOTIFICATION_SETTINGS')")
    @GetMapping
    public ResponseEntity<List<NotificationSettingResponse>> getAll() {
        return ResponseEntity.ok(settingsService.getAll());
    }

    @PreAuthorize("hasAuthority('UPDATE_NOTIFICATION_SETTINGS')")
    @PutMapping("/{eventType}")
    public ResponseEntity<NotificationSettingResponse> update(@PathVariable String eventType,
            @Valid @RequestBody NotificationSettingUpdateRequest request) {

        return ResponseEntity.ok(settingsService.update(eventType, request.enabled()));
    }
}

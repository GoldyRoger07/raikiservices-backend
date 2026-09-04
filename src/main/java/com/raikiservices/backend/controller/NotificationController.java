package com.raikiservices.backend.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.notification.NotificationResponse;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.NotificationService;
import com.raikiservices.backend.service.SseEmitterService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    public NotificationController(NotificationService notificationService,
            SseEmitterService sseEmitterService) {
        this.notificationService = notificationService;
        this.sseEmitterService = sseEmitterService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getHistory(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        return ResponseEntity.ok(
                notificationService.getHistory(principal.getId(), page, size, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(@AuthenticationPrincipal SecurityUser principal) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(principal.getId())));
    }

    /**
     * Flux temps réel de la cloche de notifications.
     *
     * <p>{@code EventSource}, côté navigateur, ne permet pas d'ajouter d'en-tête : le jeton
     * d'accès se transmet donc en paramètre {@code access_token}, cf. JwtAuthenticationFilter.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal SecurityUser principal) {
        return sseEmitterService.subscribe(principal.getId());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
            @AuthenticationPrincipal SecurityUser principal) {

        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal SecurityUser principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.noContent().build();
    }
}

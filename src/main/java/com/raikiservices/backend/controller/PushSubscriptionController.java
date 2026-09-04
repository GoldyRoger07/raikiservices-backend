package com.raikiservices.backend.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.notification.PushSubscriptionRequest;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.WebPushService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/push")
public class PushSubscriptionController {

    private final WebPushService webPushService;

    public PushSubscriptionController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    /** Clé publique VAPID attendue par PushManager.subscribe() côté navigateur. */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, Object>> getPublicKey() {
        return ResponseEntity.ok(Map.of(
                "publicKey", webPushService.getPublicKey(),
                "enabled", webPushService.isEnabled()));
    }

    @PostMapping("/subscription")
    public ResponseEntity<Void> subscribe(@Valid @RequestBody PushSubscriptionRequest request,
            @AuthenticationPrincipal SecurityUser principal,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

        webPushService.subscribe(principal.getUser(), request, userAgent);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscription")
    public ResponseEntity<Void> unsubscribe(@RequestParam String endpoint) {
        webPushService.unsubscribe(endpoint);
        return ResponseEntity.noContent().build();
    }
}

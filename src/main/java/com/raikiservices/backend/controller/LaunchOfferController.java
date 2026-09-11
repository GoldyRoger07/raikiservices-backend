package com.raikiservices.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.offer.LaunchOfferRequest;
import com.raikiservices.backend.dto.offer.LaunchOfferResponse;
import com.raikiservices.backend.service.LaunchOfferService;

import jakarta.validation.Valid;

/**
 * Offre de lancement affichée sur la page des tarifs.
 *
 * <p>La face publique est interrogée à chaque visite de la page : elle ne renvoie que des
 * compteurs, aucune donnée sensible.
 */
@RestController
public class LaunchOfferController {

    private final LaunchOfferService launchOfferService;

    public LaunchOfferController(LaunchOfferService launchOfferService) {
        this.launchOfferService = launchOfferService;
    }

    @GetMapping("/public/v1/launch-offer")
    public ResponseEntity<LaunchOfferResponse> getPublic() {
        return ResponseEntity.ok(launchOfferService.get());
    }

    @PreAuthorize("hasAuthority('READ_SETTING')")
    @GetMapping("/api/v1/launch-offer")
    public ResponseEntity<LaunchOfferResponse> get() {
        return ResponseEntity.ok(launchOfferService.get());
    }

    @PreAuthorize("hasAuthority('UPDATE_SETTING')")
    @PutMapping("/api/v1/launch-offer")
    public ResponseEntity<LaunchOfferResponse> update(
            @Valid @RequestBody LaunchOfferRequest request) {
        return ResponseEntity.ok(launchOfferService.update(request));
    }
}

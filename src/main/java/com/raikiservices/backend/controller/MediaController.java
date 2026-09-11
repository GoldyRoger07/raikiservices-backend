package com.raikiservices.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.media.MediaAssetResponse;
import com.raikiservices.backend.dto.media.MediaRegisterRequest;
import com.raikiservices.backend.dto.media.MediaUpdateRequest;
import com.raikiservices.backend.dto.media.UploadSignatureResponse;
import com.raikiservices.backend.security.SecurityUser;
import com.raikiservices.backend.service.MediaService;

import jakarta.validation.Valid;

/**
 * Bibliothèque d'images du back-office.
 *
 * <p>Aucune face publique : les images sont livrées par le CDN de Cloudinary, le site
 * vitrine n'a donc jamais à interroger ces routes. Le fichier lui-même ne passe pas par
 * ici — le navigateur demande une signature, téléverse en direct, puis déclare le résultat.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Autorisation d'envoi direct vers Cloudinary.
     *
     * @param folder sous-dossier facultatif, rangé sous le dossier racine configuré
     */
    @PreAuthorize("hasAuthority('CREATE_MEDIA')")
    @PostMapping("/signature")
    public ResponseEntity<UploadSignatureResponse> signUpload(
            @RequestParam(defaultValue = "") String folder) {

        return ResponseEntity.ok(mediaService.signUpload(folder));
    }

    /** Déclaration d'une image téléversée, à appeler avec la réponse de Cloudinary. */
    @PreAuthorize("hasAuthority('CREATE_MEDIA')")
    @PostMapping
    public ResponseEntity<MediaAssetResponse> register(
            @Valid @RequestBody MediaRegisterRequest request,
            @AuthenticationPrincipal SecurityUser principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mediaService.register(request, principal.getId()));
    }

    @PreAuthorize("hasAuthority('READ_MEDIA')")
    @GetMapping
    public ResponseEntity<PageResponse<MediaAssetResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter,
            @RequestParam(defaultValue = "") String folder) {

        return ResponseEntity.ok(mediaService.getAll(
                new PageQuery(page, size, sortField, sortOrder, globalFilter), folder));
    }

    @PreAuthorize("hasAuthority('READ_MEDIA')")
    @GetMapping("/{id}")
    public ResponseEntity<MediaAssetResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mediaService.getById(id));
    }

    @PreAuthorize("hasAuthority('UPDATE_MEDIA')")
    @PutMapping("/{id}")
    public ResponseEntity<MediaAssetResponse> update(@PathVariable Long id,
            @Valid @RequestBody MediaUpdateRequest request) {
        return ResponseEntity.ok(mediaService.update(id, request));
    }

    /** Supprime l'image chez Cloudinary et sa fiche. Refusé si un projet l'affiche. */
    @PreAuthorize("hasAuthority('DELETE_MEDIA')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

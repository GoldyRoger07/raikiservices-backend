package com.raikiservices.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.contact.ContactMessageRequest;
import com.raikiservices.backend.dto.contact.ContactMessageResponse;
import com.raikiservices.backend.dto.contact.ContactMessageUpdateRequest;
import com.raikiservices.backend.service.ContactService;

import jakarta.validation.Valid;

@RestController
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    // ──────────────── Formulaire public ────────────────

    /**
     * Répond 202 : le message est enregistré, mais les emails partent en arrière-plan.
     * La réponse ne contient aucune donnée interne.
     */
    @PostMapping("/public/v1/contact")
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody ContactMessageRequest request) {
        contactService.submit(request);
        return ResponseEntity.accepted()
                .body(Map.of("message", "Votre message a bien été envoyé. Nous vous répondrons rapidement."));
    }

    // ──────────────── Back-office ────────────────

    @PreAuthorize("hasAuthority('READ_CONTACT')")
    @GetMapping("/api/v1/contact")
    public ResponseEntity<PageResponse<ContactMessageResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "") String globalFilter) {

        return ResponseEntity.ok(contactService.getAll(
                new PageQuery(page, size, sortField, sortOrder, globalFilter)));
    }

    @PreAuthorize("hasAuthority('READ_CONTACT')")
    @GetMapping("/api/v1/contact/unread-count")
    public ResponseEntity<Map<String, Long>> countNew() {
        return ResponseEntity.ok(Map.of("count", contactService.countNew()));
    }

    @PreAuthorize("hasAuthority('READ_CONTACT')")
    @GetMapping("/api/v1/contact/{id}")
    public ResponseEntity<ContactMessageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getById(id));
    }

    @PreAuthorize("hasAuthority('UPDATE_CONTACT')")
    @PutMapping("/api/v1/contact/{id}")
    public ResponseEntity<ContactMessageResponse> update(@PathVariable Long id,
            @Valid @RequestBody ContactMessageUpdateRequest request) {
        return ResponseEntity.ok(contactService.update(id, request));
    }

    @PreAuthorize("hasAuthority('DELETE_CONTACT')")
    @DeleteMapping("/api/v1/contact/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

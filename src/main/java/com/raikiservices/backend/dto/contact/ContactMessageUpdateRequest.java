package com.raikiservices.backend.dto.contact;

import com.raikiservices.backend.entity.ContactStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Champs de suivi modifiables depuis le back-office. Le message lui-même reste immuable. */
public record ContactMessageUpdateRequest(
        @NotNull(message = "Le statut est obligatoire.")
        ContactStatus status,

        @Size(max = 5000, message = "Les notes ne peuvent pas dépasser 5000 caractères.")
        String adminNotes) {
}

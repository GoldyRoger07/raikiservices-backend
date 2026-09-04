package com.raikiservices.backend.dto.notification;

import jakarta.validation.constraints.NotBlank;

/** Les canaux omis valent {@code false} : le client peut n'envoyer que ceux qu'il active. */
public record NotificationPreferenceUpdateRequest(
        @NotBlank(message = "Le type d'evenement est obligatoire.")
        String eventType,
        Boolean inApp,
        Boolean email,
        Boolean push) {

    public NotificationPreferenceUpdateRequest {
        inApp = inApp != null && inApp;
        email = email != null && email;
        push = push != null && push;
    }
}

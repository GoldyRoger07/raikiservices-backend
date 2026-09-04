package com.raikiservices.backend.dto.notification;

import java.time.Instant;

import com.raikiservices.backend.entity.Notification;

/** @param read état lu pour le destinataire qui consulte, pas pour tous */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Long entityId,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n, Long viewerId) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getEntityId(),
                n.getReadBy().contains(viewerId),
                n.getCreatedAt());
    }
}

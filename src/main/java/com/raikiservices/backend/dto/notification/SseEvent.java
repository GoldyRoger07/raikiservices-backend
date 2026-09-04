package com.raikiservices.backend.dto.notification;

/**
 * Charge utile d'un évènement temps réel.
 *
 * @param type     code d'un NotificationType
 * @param entityId objet concerné, pour permettre au client d'ouvrir le bon écran
 */
public record SseEvent(String type, String title, String message, Long entityId) {
}

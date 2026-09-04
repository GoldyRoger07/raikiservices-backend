package com.raikiservices.backend.dto.notification;

import com.raikiservices.backend.entity.NotificationPreference;
import com.raikiservices.backend.entity.NotificationType;

public record NotificationPreferenceResponse(
        String eventType,
        String label,
        boolean inApp,
        boolean email,
        boolean push) {

    public static NotificationPreferenceResponse from(NotificationPreference pref, NotificationType type) {
        return new NotificationPreferenceResponse(
                pref.getEventType(),
                type.getLabel(),
                pref.isInApp(),
                pref.isEmail(),
                pref.isPush());
    }
}

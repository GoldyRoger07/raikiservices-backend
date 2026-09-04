package com.raikiservices.backend.dto.notification;

import com.raikiservices.backend.entity.NotificationSetting;

public record NotificationSettingResponse(String eventType, String label, boolean enabled) {

    public static NotificationSettingResponse from(NotificationSetting setting) {
        return new NotificationSettingResponse(
                setting.getEventType(), setting.getLabel(), setting.isEnabled());
    }
}

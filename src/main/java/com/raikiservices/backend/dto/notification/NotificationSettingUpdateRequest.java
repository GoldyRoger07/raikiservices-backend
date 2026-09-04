package com.raikiservices.backend.dto.notification;

/** Un champ {@code enabled} absent vaut {@code false} plutot que de provoquer un 400. */
public record NotificationSettingUpdateRequest(Boolean enabled) {

    public NotificationSettingUpdateRequest {
        enabled = enabled != null && enabled;
    }
}

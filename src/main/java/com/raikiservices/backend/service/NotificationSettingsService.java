package com.raikiservices.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.notification.NotificationSettingResponse;
import com.raikiservices.backend.entity.NotificationSetting;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.NotificationSettingRepository;

/**
 * Interrupteurs globaux : quels types d'évènements produisent une notification.
 *
 * <p>Les lignes manquantes sont créées à la volée à l'état activé, ce qui évite un seeder
 * dédié et fait apparaître automatiquement tout nouveau type ajouté à l'enum.
 */
@Service
public class NotificationSettingsService {

    private final NotificationSettingRepository repository;

    public NotificationSettingsService(NotificationSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<NotificationSettingResponse> getAll() {
        return java.util.Arrays.stream(NotificationType.values())
                .map(this::resolve)
                .map(NotificationSettingResponse::from)
                .toList();
    }

    @Transactional
    public NotificationSettingResponse update(String eventType, boolean enabled) {
        NotificationType type = NotificationType.fromCode(eventType)
                .orElseThrow(() -> ResourceNotFoundException.of("Type de notification", eventType));

        NotificationSetting setting = resolve(type);
        setting.setEnabled(enabled);
        return NotificationSettingResponse.from(repository.save(setting));
    }

    /** Un type inconnu est considéré désactivé : mieux vaut ne rien émettre que d'émettre à tort. */
    @Transactional(readOnly = true)
    public boolean isEnabled(String eventType) {
        return NotificationType.fromCode(eventType)
                .map(type -> repository.findByEventType(type.getCode())
                        .map(NotificationSetting::isEnabled)
                        .orElse(true))
                .orElse(false);
    }

    private NotificationSetting resolve(NotificationType type) {
        return repository.findByEventType(type.getCode()).orElseGet(() -> {
            NotificationSetting created = new NotificationSetting();
            created.setEventType(type.getCode());
            created.setLabel(type.getLabel());
            created.setEnabled(true);
            return repository.save(created);
        });
    }
}

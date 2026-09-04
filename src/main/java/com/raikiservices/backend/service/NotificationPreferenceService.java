package com.raikiservices.backend.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.notification.NotificationPreferenceResponse;
import com.raikiservices.backend.dto.notification.NotificationPreferenceUpdateRequest;
import com.raikiservices.backend.entity.NotificationPreference;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.NotificationPreferenceRepository;

/**
 * Préférences de canal, par utilisateur et par type d'évènement.
 *
 * <p>Une préférence absente n'est pas une erreur : elle vaut les valeurs par défaut de
 * {@link NotificationPreference} — in-app et email activés, push désactivé. La ligne n'est
 * écrite en base que lorsque l'utilisateur modifie effectivement un réglage.
 */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getForUser(User user) {
        return Arrays.stream(NotificationType.values())
                .map(type -> NotificationPreferenceResponse.from(resolve(user, type), type))
                .toList();
    }

    @Transactional
    public NotificationPreferenceResponse update(User user, NotificationPreferenceUpdateRequest request) {
        NotificationType type = NotificationType.fromCode(request.eventType())
                .orElseThrow(() -> ResourceNotFoundException.of("Type de notification", request.eventType()));

        NotificationPreference preference = repository.findByUserAndEventType(user, type.getCode())
                .orElseGet(() -> {
                    NotificationPreference created = new NotificationPreference();
                    created.setUser(user);
                    created.setEventType(type.getCode());
                    return created;
                });

        preference.setInApp(request.inApp());
        preference.setEmail(request.email());
        preference.setPush(request.push());

        return NotificationPreferenceResponse.from(repository.save(preference), type);
    }

    /**
     * Préférence effective d'un utilisateur pour un type, sans jamais écrire en base : appelée
     * à chaque émission, elle ne doit pas créer une ligne par destinataire et par évènement.
     */
    @Transactional(readOnly = true)
    public NotificationPreference resolve(User user, NotificationType type) {
        return repository.findByUserAndEventType(user, type.getCode())
                .orElseGet(() -> {
                    NotificationPreference defaults = new NotificationPreference();
                    defaults.setUser(user);
                    defaults.setEventType(type.getCode());
                    return defaults;
                });
    }
}

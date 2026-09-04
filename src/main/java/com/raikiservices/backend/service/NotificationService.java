package com.raikiservices.backend.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.notification.NotificationResponse;
import com.raikiservices.backend.dto.notification.SseEvent;
import com.raikiservices.backend.entity.Notification;
import com.raikiservices.backend.entity.NotificationPreference;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.NotificationRepository;
import com.raikiservices.backend.repository.UserRepository;

/**
 * Point d'entrée unique de l'émission de notifications : archive l'évènement, le pousse en
 * temps réel, et le relaie par email ou en push selon les préférences du destinataire.
 */
@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final SseEmitterService sseEmitterService;
    private final NotificationSettingsService settingsService;
    private final NotificationPreferenceService preferenceService;
    private final EmailService emailService;
    private final WebPushService webPushService;
    private final String frontendUrl;

    public NotificationService(NotificationRepository repository,
            UserRepository userRepository,
            SseEmitterService sseEmitterService,
            NotificationSettingsService settingsService,
            NotificationPreferenceService preferenceService,
            EmailService emailService,
            WebPushService webPushService,
            @Value("${app.frontend-url}") String frontendUrl) {

        this.repository = repository;
        this.userRepository = userRepository;
        this.sseEmitterService = sseEmitterService;
        this.settingsService = settingsService;
        this.preferenceService = preferenceService;
        this.emailService = emailService;
        this.webPushService = webPushService;
        this.frontendUrl = frontendUrl;
    }

    // ──────────────── Émission ────────────────

    /**
     * Diffuse un évènement à tous les comptes habilités à le recevoir, c'est-à-dire porteurs
     * de la permission associée au type.
     */
    @Transactional
    public void dispatch(SseEvent event) {
        if (!settingsService.isEnabled(event.type())) {
            return;
        }

        NotificationType type = NotificationType.fromCode(event.type()).orElse(null);
        if (type == null || type.getRequiredPermission() == null) {
            return;
        }

        for (User recipient : userRepository.findByPermission(type.getRequiredPermission())) {
            deliver(recipient, event, type);
        }
    }

    /** Notification adressée nominativement. */
    @Transactional
    public void notifyUser(Long userId, SseEvent event) {
        if (!settingsService.isEnabled(event.type())) {
            return;
        }

        NotificationType type = NotificationType.fromCode(event.type()).orElse(null);
        if (type == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> deliver(user, event, type));
    }

    /** Applique, pour un destinataire donné, les seuls canaux qu'il a activés. */
    private void deliver(User recipient, SseEvent event, NotificationType type) {
        NotificationPreference preference = preferenceService.resolve(recipient, type);
        String link = frontendUrl + type.getRoute();

        if (preference.isInApp()) {
            persist(event, recipient.getId());
            sseEmitterService.send(recipient.getId(), event);
        }
        if (preference.isEmail() && recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendNotificationEmail(recipient.getEmail(), event.title(), event.message(), link);
        }
        if (preference.isPush()) {
            webPushService.sendTo(recipient, event.title(), event.message(), link);
        }
    }

    private void persist(SseEvent event, Long recipientId) {
        Notification notification = new Notification();
        notification.setType(event.type());
        notification.setTitle(event.title());
        notification.setMessage(event.message());
        notification.setEntityId(event.entityId());
        notification.setRecipientId(recipientId);
        repository.save(notification);
    }

    // ──────────────── Consultation ────────────────

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getHistory(Long userId, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> result = unreadOnly
                ? repository.findVisibleUnread(userId, pageable)
                : repository.findVisible(userId, pageable);

        return PageResponse.from(result, n -> NotificationResponse.from(n, userId));
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return repository.countUnread(userId);
    }

    // ──────────────── État lu / non lu ────────────────

    @Transactional
    public void markAsRead(Long id, Long userId) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));

        // Une notification nominative n'est lisible que par son destinataire. On renvoie 404
        // plutôt que 403 : confirmer l'existence de la notification renseignerait déjà.
        if (notification.getRecipientId() != null && !notification.getRecipientId().equals(userId)) {
            throw ResourceNotFoundException.of("Notification", id);
        }

        notification.getReadBy().add(userId);
        repository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = repository.findAllVisibleUnread(userId);
        unread.forEach(notification -> notification.getReadBy().add(userId));
        repository.saveAll(unread);
    }
}

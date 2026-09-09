package com.raikiservices.backend.service;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.raikiservices.backend.dto.common.PageQuery;
import com.raikiservices.backend.dto.common.PageResponse;
import com.raikiservices.backend.dto.contact.ContactMessageRequest;
import com.raikiservices.backend.dto.contact.ContactMessageResponse;
import com.raikiservices.backend.dto.contact.ContactMessageUpdateRequest;
import com.raikiservices.backend.dto.notification.SseEvent;
import com.raikiservices.backend.entity.ContactMessage;
import com.raikiservices.backend.entity.ContactStatus;
import com.raikiservices.backend.entity.NotificationType;
import com.raikiservices.backend.exception.ResourceNotFoundException;
import com.raikiservices.backend.repository.ContactMessageRepository;

@Service
public class ContactService {

    /** Champs sur lesquels le client peut trier — le paramètre finit dans un ORDER BY. */
    private static final Set<String> SORTABLE = Set.of(
            "firstName", "lastName", "email", "companyName", "serviceCategory",
            "subject", "status", "submittedAt");

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final String recipientEmail;

    public ContactService(ContactMessageRepository contactMessageRepository,
            EmailService emailService,
            NotificationService notificationService,
            @Value("${app.contact.recipient:${app.mail.from}}") String recipientEmail) {

        this.contactMessageRepository = contactMessageRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.recipientEmail = recipientEmail;
    }

    // ──────────────── Dépôt public ────────────────

    /**
     * Enregistre le message puis déclenche les deux emails — alerte à l'agence, accusé de
     * réception au visiteur.
     *
     * <p>Les envois étant asynchrones et silencieux en cas d'échec, une panne du fournisseur
     * d'emails ne fait pas perdre le message : il est déjà en base quand la méthode rend la main.
     *
     * <p>Ne renvoie rien : l'identifiant et le statut interne ne regardent pas le visiteur.
     */
    @Transactional
    public void submit(ContactMessageRequest request) {
        ContactMessage entity = new ContactMessage();
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setCompanyName(request.companyName());
        entity.setServiceCategory(request.serviceCategory());
        entity.setSubject(request.subject());
        entity.setMessage(request.message());
        entity.setNewsletterOptIn(request.newsletterOptIn());

        ContactMessage saved = contactMessageRepository.save(entity);

        String senderName = displayName(saved);

        // Previent dans le back-office tout compte porteur de READ_CONTACT.
        notificationService.dispatch(new SseEvent(
                NotificationType.NEW_CONTACT.getCode(),
                "Nouveau message de contact",
                senderName + " - " + subjectOrDefault(saved),
                saved.getId()));

        emailService.sendContactNotification(
                recipientEmail, senderName, saved.getEmail(),
                subjectOrDefault(saved), saved.getMessage(), saved.getId());
        emailService.sendContactConfirmation(saved.getEmail(), senderName);
    }

    // ──────────────── Back-office ────────────────

    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getAll(PageQuery query) {
        return getAll(query, null);
    }

    /**
     * Page de messages, filtree par texte libre et / ou par statut.
     *
     * @param status statut exige, ou {@code null} pour ne pas filtrer dessus
     */
    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> getAll(PageQuery query, ContactStatus status) {
        Pageable pageable = query.toPageable(SORTABLE, "submittedAt");
        String filter = query.filterOrNull();

        Page<ContactMessage> result;
        if (filter == null) {
            result = status == null
                    ? contactMessageRepository.findAll(pageable)
                    : contactMessageRepository.findByStatus(status, pageable);
        } else {
            result = status == null
                    ? contactMessageRepository.search(filter, pageable)
                    : contactMessageRepository.searchByStatus(filter, status, pageable);
        }

        return PageResponse.from(result, ContactMessageResponse::summaryFrom);
    }

    @Transactional(readOnly = true)
    public ContactMessageResponse getById(Long id) {
        return ContactMessageResponse.from(findOrThrow(id));
    }

    /** Nombre de messages non encore traités — alimente le badge du back-office. */
    @Transactional(readOnly = true)
    public long countNew() {
        return contactMessageRepository.countByStatus(ContactStatus.NEW);
    }

    @Transactional
    public ContactMessageResponse update(Long id, ContactMessageUpdateRequest request) {
        ContactMessage message = findOrThrow(id);
        message.setStatus(request.status());
        message.setAdminNotes(request.adminNotes());
        return ContactMessageResponse.from(contactMessageRepository.save(message));
    }

    @Transactional
    public void delete(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Message de contact", id);
        }
        contactMessageRepository.deleteById(id);
    }

    // ──────────────── Interne ────────────────

    private ContactMessage findOrThrow(Long id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Message de contact", id));
    }

    private String displayName(ContactMessage message) {
        String first = message.getFirstName() == null ? "" : message.getFirstName();
        String last = message.getLastName() == null ? "" : message.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? message.getEmail() : full;
    }

    private String subjectOrDefault(ContactMessage message) {
        String subject = message.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        String category = message.getServiceCategory();
        return (category != null && !category.isBlank()) ? category : "Demande de contact";
    }
}

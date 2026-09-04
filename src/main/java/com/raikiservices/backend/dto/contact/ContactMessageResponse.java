package com.raikiservices.backend.dto.contact;

import java.time.Instant;

import com.raikiservices.backend.entity.ContactMessage;
import com.raikiservices.backend.entity.ContactStatus;

public record ContactMessageResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String companyName,
        String serviceCategory,
        String subject,
        String message,
        boolean newsletterOptIn,
        ContactStatus status,
        String adminNotes,
        Instant submittedAt) {

    public static ContactMessageResponse from(ContactMessage c) {
        return new ContactMessageResponse(
                c.getId(),
                c.getFirstName(),
                c.getLastName(),
                c.getEmail(),
                c.getPhone(),
                c.getCompanyName(),
                c.getServiceCategory(),
                c.getSubject(),
                c.getMessage(),
                c.isNewsletterOptIn(),
                c.getStatus(),
                c.getAdminNotes(),
                c.getSubmittedAt());
    }

    /** Variante de liste : le corps du message est tronqué pour alléger les pages. */
    public static ContactMessageResponse summaryFrom(ContactMessage c) {
        String body = c.getMessage();
        String preview = (body != null && body.length() > 160) ? body.substring(0, 160) + "…" : body;

        return new ContactMessageResponse(
                c.getId(), c.getFirstName(), c.getLastName(), c.getEmail(), c.getPhone(),
                c.getCompanyName(), c.getServiceCategory(), c.getSubject(), preview,
                c.isNewsletterOptIn(), c.getStatus(), c.getAdminNotes(), c.getSubmittedAt());
    }
}

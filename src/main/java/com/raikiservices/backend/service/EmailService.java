package com.raikiservices.backend.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Envoi d'emails transactionnels via l'API HTTP de Resend.
 *
 * <p>Tous les envois sont {@code @Async} : un fournisseur lent ou indisponible ne doit
 * jamais retarder — ni faire échouer — la requête métier qui a déclenché l'email. Les
 * erreurs sont donc journalisées, pas propagées.
 *
 * <p>{@code app.mail.console-mode=true} remplace l'envoi réel par une trace dans les logs,
 * ce qui permet de développer le parcours d'inscription sans consommer de quota.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String BRAND = "Raiki Services";

    private final RestClient restClient = RestClient.create();

    private final String fromAddress;
    private final String baseUrl;
    private final String frontendUrl;
    private final boolean consoleMode;
    private final String resendApiKey;

    public EmailService(
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail.console-mode:false}") boolean consoleMode,
            @Value("${resend.api-key}") String resendApiKey) {

        this.fromAddress = fromAddress;
        this.baseUrl = baseUrl;
        this.frontendUrl = frontendUrl;
        this.consoleMode = consoleMode;
        this.resendApiKey = resendApiKey;
    }

    // ──────────────── Emails métier ────────────────

    /** Code OTP et lien cliquable pour valider une adresse email à l'inscription. */
    @Async
    public void sendVerificationEmail(String toEmail, String username, String otpCode, String linkToken) {
        String link = baseUrl + "/api/v1/auth/verify-email?token=" + linkToken;

        Map<String, String> trace = new LinkedHashMap<>();
        trace.put("A", toEmail);
        trace.put("Utilisateur", username);
        trace.put("Code OTP", otpCode);
        trace.put("Lien", link);
        if (logToConsole("VERIFICATION EMAIL", trace)) {
            return;
        }

        String html = card(
                "Vérifiez votre adresse email",
                "Bonjour " + escape(username) + ", confirmez votre adresse pour activer votre compte "
                        + BRAND + ".",
                otpBlock(otpCode),
                button(link, "Vérifier mon adresse"),
                "Ce code expire dans 15 minutes. Si vous n'êtes pas à l'origine de cette demande, "
                        + "ignorez cet email.");

        deliver(toEmail, "Vérifiez votre adresse email — " + BRAND, html, null);
    }

    /** Lien de réinitialisation de mot de passe, à ouvrir dans le frontend. */
    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;

        Map<String, String> trace = new LinkedHashMap<>();
        trace.put("A", toEmail);
        trace.put("Utilisateur", username);
        trace.put("Lien", link);
        if (logToConsole("REINITIALISATION MOT DE PASSE", trace)) {
            return;
        }

        String html = card(
                "Réinitialisation de votre mot de passe",
                "Bonjour " + escape(username) + ", vous avez demandé à réinitialiser votre mot de passe.",
                "",
                button(link, "Choisir un nouveau mot de passe"),
                "Ce lien expire dans 30 minutes. Si vous n'avez rien demandé, aucune action n'est "
                        + "nécessaire : votre mot de passe reste inchangé.");

        deliver(toEmail, "Réinitialisation de votre mot de passe — " + BRAND, html, null);
    }

    /**
     * Prévient l'agence qu'un message de contact vient d'arriver.
     *
     * <p>Le champ {@code reply_to} porte l'adresse du visiteur : répondre depuis la boîte de
     * réception écrit directement au prospect, sans copier-coller.
     */
    @Async
    public void sendContactNotification(String toEmail, String senderName, String senderEmail,
            String subject, String message, Long messageId) {

        String link = frontendUrl + "/admin/contact/" + messageId;

        Map<String, String> trace = new LinkedHashMap<>();
        trace.put("A", toEmail);
        trace.put("De", senderName + " <" + senderEmail + ">");
        trace.put("Sujet", String.valueOf(subject));
        trace.put("Message", message);
        if (logToConsole("NOUVEAU MESSAGE DE CONTACT", trace)) {
            return;
        }

        String body = "<p style=\"margin:16px 0 4px;color:#888;font-size:13px;\">Sujet</p>"
                + "<p style=\"margin:0 0 16px;color:#333;\">" + escape(subject) + "</p>"
                + "<p style=\"margin:0 0 4px;color:#888;font-size:13px;\">Message</p>"
                + "<p style=\"margin:0;color:#333;white-space:pre-wrap;\">" + escape(message) + "</p>";

        String html = card(
                "Nouveau message de contact",
                "<strong>" + escape(senderName) + "</strong> (" + escape(senderEmail) + ") vous a écrit.",
                body,
                button(link, "Ouvrir dans l'administration"),
                "Vous pouvez répondre directement à cet email pour écrire au visiteur.");

        deliver(toEmail, "Nouveau message de " + senderName + " — " + BRAND, html, senderEmail);
    }

    /** Accusé de réception envoyé au visiteur qui a rempli le formulaire. */
    @Async
    public void sendContactConfirmation(String toEmail, String senderName) {
        Map<String, String> trace = new LinkedHashMap<>();
        trace.put("A", toEmail);
        trace.put("Nom", senderName);
        if (logToConsole("ACCUSE DE RECEPTION CONTACT", trace)) {
            return;
        }

        String html = card(
                "Nous avons bien reçu votre message",
                "Bonjour " + escape(senderName) + ", merci de nous avoir contactés.",
                "<p style=\"color:#555;font-size:15px;margin:16px 0 0;\">Notre équipe revient vers vous "
                        + "sous 48 heures ouvrées.</p>",
                "",
                "Cet email est automatique, inutile d'y répondre.");

        deliver(toEmail, "Votre message a bien été reçu — " + BRAND, html, null);
    }

    /** Notification système générique, utilisée par le module de notifications. */
    @Async
    public void sendNotificationEmail(String toEmail, String title, String message, String link) {
        Map<String, String> trace = new LinkedHashMap<>();
        trace.put("A", toEmail);
        trace.put("Titre", title);
        trace.put("Message", message);
        trace.put("Lien", String.valueOf(link));
        if (logToConsole("NOTIFICATION", trace)) {
            return;
        }

        String html = card(
                title,
                escape(message),
                "",
                link == null ? "" : button(link, "Ouvrir dans l'application"),
                "Vous recevez cet email selon vos préférences de notification, "
                        + "modifiables dans Paramètres.");

        deliver(toEmail, title + " — " + BRAND, html, null);
    }

    // ──────────────── Transport ────────────────

    private void deliver(String to, String subject, String html, String replyTo) {
        Map<String, Object> body = new HashMap<>();
        body.put("from", fromAddress);
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);
        if (replyTo != null) {
            body.put("reply_to", replyTo);
        }

        try {
            restClient.post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Échec de l'envoi de l'email [{}] à {} : {}", subject, to, e.getMessage());
        }
    }

    private boolean logToConsole(String kind, Map<String, String> fields) {
        if (!consoleMode) {
            return false;
        }
        log.info("--------------- {} [mode console] ---------------", kind);
        fields.forEach((k, v) -> log.info("  {} : {}", k, v));
        log.info("------------------------------------------------");
        return true;
    }

    // ──────────────── Gabarits HTML ────────────────

    private String card(String title, String intro, String extraBlock, String actionBlock, String footer) {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;font-family:Arial,Helvetica,sans-serif;background:#f4f5f7;padding:24px;">
                  <div style="max-width:540px;margin:auto;background:#ffffff;border-radius:10px;
                              padding:32px;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                    <h2 style="color:#1f2933;margin:0 0 12px;font-size:20px;">%s</h2>
                    <p style="color:#555;font-size:15px;line-height:1.5;margin:0;">%s</p>
                    %s
                    %s
                    <hr style="border:none;border-top:1px solid #eceff1;margin:28px 0 16px;">
                    <p style="color:#9aa5b1;font-size:12px;line-height:1.5;margin:0;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(escape(title), intro, extraBlock, actionBlock, footer);
    }

    private String otpBlock(String otpCode) {
        return """
                <div style="margin:24px 0;text-align:center;">
                  <div style="display:inline-block;background:#f4f5f7;border-radius:8px;padding:16px 32px;
                              font-size:30px;letter-spacing:8px;font-weight:bold;color:#1f2933;">%s</div>
                </div>
                """.formatted(escape(otpCode));
    }

    private String button(String link, String label) {
        return """
                <div style="margin:28px 0;text-align:center;">
                  <a href="%s" style="display:inline-block;background:#4a90e2;color:#ffffff;
                     text-decoration:none;padding:13px 30px;border-radius:6px;font-size:15px;
                     font-weight:bold;">%s</a>
                </div>
                """.formatted(link, escape(label));
    }

    /** Neutralise le HTML des données saisies : un message de contact ne doit rien injecter. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

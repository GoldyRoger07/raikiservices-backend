package com.raikiservices.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;

import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.raikiservices.backend.dto.notification.PushSubscriptionRequest;
import com.raikiservices.backend.entity.PushSubscription;
import com.raikiservices.backend.entity.User;
import com.raikiservices.backend.repository.PushSubscriptionRepository;

import jakarta.annotation.PostConstruct;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

/**
 * Abonnements Web Push (VAPID) et envoi des notifications navigateur.
 *
 * <p>Sans clés VAPID configurées, le canal se désactive proprement : l'application démarre et
 * les envois sont ignorés, plutôt que d'échouer au démarrage.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final PushSubscriptionRepository repository;
    private final String publicKey;
    private final String privateKey;
    private final String subject;

    private PushService pushService;

    public WebPushService(PushSubscriptionRepository repository,
            @Value("${vapid.public-key:}") String publicKey,
            @Value("${vapid.private-key:}") String privateKey,
            @Value("${vapid.subject:}") String subject) {

        this.repository = repository;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    @PostConstruct
    void init() {
        // Le chiffrement ECDH du Web Push passe par BouncyCastle, absent des fournisseurs JCE
        // par défaut de la JVM.
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        if (!StringUtils.hasText(publicKey) || !StringUtils.hasText(privateKey)) {
            log.warn("Web Push désactivé — clés VAPID absentes "
                    + "(vapid.public-key / vapid.private-key).");
            return;
        }

        try {
            this.pushService = new PushService(publicKey, privateKey, subject);
            log.info("Web Push activé (VAPID configuré).");
        } catch (Exception e) {
            log.error("Web Push désactivé — clés VAPID invalides : {}", e.getMessage());
        }
    }

    /** Clé publique à transmettre au navigateur pour qu'il s'abonne. Vide si push désactivé. */
    public String getPublicKey() {
        return publicKey == null ? "" : publicKey;
    }

    public boolean isEnabled() {
        return pushService != null;
    }

    // ──────────────── Abonnements ────────────────

    /**
     * Enregistre ou met à jour l'abonnement d'un navigateur.
     *
     * <p>Un même endpoint réabonné écrase la ligne existante : le navigateur peut faire tourner
     * ses clés sans qu'on accumule des abonnements morts.
     */
    @Transactional
    public void subscribe(User user, PushSubscriptionRequest request, String userAgent) {
        PushSubscription subscription = repository.findByEndpoint(request.endpoint())
                .orElseGet(PushSubscription::new);

        subscription.setUser(user);
        subscription.setEndpoint(request.endpoint());
        subscription.setP256dh(request.keys().p256dh());
        subscription.setAuth(request.keys().auth());
        subscription.setUserAgent(truncate(userAgent));
        repository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String endpoint) {
        if (StringUtils.hasText(endpoint)) {
            repository.deleteByEndpoint(endpoint);
        }
    }

    // ──────────────── Envoi ────────────────

    /** Pousse une notification sur tous les appareils abonnés d'un compte. */
    @Async
    @Transactional
    public void sendTo(User user, String title, String message, String url) {
        if (pushService == null) {
            return;
        }

        List<PushSubscription> subscriptions = repository.findByUser(user);
        if (subscriptions.isEmpty()) {
            return;
        }

        byte[] payload = buildPayload(title, message, url).getBytes(StandardCharsets.UTF_8);

        for (PushSubscription subscription : subscriptions) {
            try {
                HttpResponse response = pushService.send(new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload));

                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    // Abonnement révoqué côté navigateur : il ne servira plus jamais.
                    repository.delete(subscription);
                } else if (status >= 400) {
                    log.warn("Push refusé (HTTP {}) pour l'abonnement {}.", status, subscription.getId());
                }
            } catch (Exception e) {
                log.warn("Échec de l'envoi push pour l'abonnement {} : {}",
                        subscription.getId(), e.getMessage());
            }
        }
    }

    /**
     * Construit le JSON lu par le service worker. Écrit à la main pour ne pas dépendre d'une
     * version particulière de Jackson dans un service par ailleurs sans sérialisation.
     */
    private String buildPayload(String title, String message, String url) {
        return "{\"title\":\"" + escapeJson(title)
                + "\",\"body\":\"" + escapeJson(message)
                + "\",\"url\":\"" + escapeJson(url) + "\"}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length() + 16);
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 400 ? userAgent : userAgent.substring(0, 400);
    }
}

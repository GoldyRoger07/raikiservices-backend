package com.raikiservices.backend.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.raikiservices.backend.dto.notification.SseEvent;

/**
 * Connexions SSE ouvertes, indexées par compte.
 *
 * <p>Un même compte peut avoir plusieurs connexions — un onglet par appareil — d'où une liste
 * par identifiant. Les structures sont concurrentes : les émissions viennent des threads de
 * requête, les fermetures du conteneur, et le battement de cœur d'un thread planifié.
 *
 * <p>L'état vit en mémoire du processus : avec plusieurs instances derrière un répartiteur de
 * charge, chacune ne notifierait que ses propres connectés. Ce backend étant mono-instance,
 * c'est sans conséquence ici — mais c'est le premier point à revoir en cas de mise à l'échelle.
 */
@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    /** Au-delà, le navigateur rouvre la connexion de lui-même. */
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        return emitter;
    }

    public void send(Long userId, SseEvent event) {
        List<SseEmitter> connections = emitters.get(userId);
        if (connections != null) {
            doSend(connections, event);
        }
    }

    public void broadcast(SseEvent event) {
        emitters.values().forEach(connections -> doSend(connections, event));
    }

    /**
     * Battement de cœur : sans trafic, un proxy ou un pare-feu coupe une connexion inactive au
     * bout de quelques dizaines de secondes. L'envoi sert aussi à détecter les clients partis.
     */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        SseEmitter.SseEventBuilder ping = SseEmitter.event().name("ping").data("keep-alive");

        emitters.forEach((userId, connections) -> {
            List<SseEmitter> dead = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : connections) {
                try {
                    emitter.send(ping);
                } catch (IOException | IllegalStateException e) {
                    dead.add(emitter);
                }
            }
            connections.removeAll(dead);
            if (connections.isEmpty()) {
                emitters.remove(userId);
            }
        });
    }

    private void doSend(List<SseEmitter> connections, SseEvent event) {
        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : connections) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.type())
                        .data(event, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                // Client déconnecté entre-temps : cas courant, pas une anomalie.
                dead.add(emitter);
            } catch (Exception e) {
                log.error("Erreur d'émission SSE : {}", e.getMessage());
                dead.add(emitter);
            }
        }
        connections.removeAll(dead);
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> connections = emitters.get(userId);
        if (connections == null) {
            return;
        }
        connections.remove(emitter);
        if (connections.isEmpty()) {
            emitters.remove(userId);
        }
    }
}

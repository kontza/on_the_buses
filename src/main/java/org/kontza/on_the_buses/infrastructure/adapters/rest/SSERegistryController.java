package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SSERegistryController {
    @Value("${on-the-buses.sse-timeout}")
    private long sseTimeout;
    private Map<String, SseEmitter> clients = new HashMap<>();
    private NotifierService notifierService;

    public SSERegistryController(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @GetMapping("/register")
    public SseEmitter registerClient(@RequestParam final String clientId) {
        var emitter = new SseEmitter(sseTimeout);
        if (clients.containsKey(clientId)) {
            log.info(">>> {} was already registered. Completing the previous registration.", clientId);
            clients.get(clientId).complete();
        }
        emitter.onTimeout(() -> {
            log.error(">>> '{}' emitter timed out", clientId);
            clients.remove(clientId);
            emitter.complete();
        });
        emitter.onError(err -> {
            log.error(">>> '{}' emitter error:", clientId, err);
            clients.remove(clientId);
            emitter.complete();
        });
        emitter.onCompletion(() -> {
            log.info(">>> '{}' emitter completed", clientId);
            clients.remove(clientId);
        });
        clients.put(clientId, emitter);
        log.info(">>> Register '{}'", clientId);
        return emitter;
    }

    @GetMapping("/update")
    public void update(@RequestParam final String reason) {
        clients.values().forEach(sseEmitter -> {
            try {
                log.info(">>> Got an update request '{}'", reason);
                sseEmitter.send(reason);
                notifierService.notifier(Optional.of(reason), Optional.of(sseTimeout));
            } catch (IOException e) {
                log.error(">>> Sending '{}' failed:", reason, e);
                throw new RuntimeException(e);
            }
        });
    }
}

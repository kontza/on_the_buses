package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_COMPLETE = "COMPLETE";
    public static final String OK = "OK";
    public static final long THREE_HOURS = 10800000l;
    @Value("${on-the-buses.sse-timeout}")
    private long sseTimeout;
    private final Map<String, SseEmitter> clients = new HashMap<>();
    private final NotifierService notifierService;

    public SSERegistryController(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @GetMapping("/unregister")
    public ResponseEntity<String> unregisterClient(@RequestParam final String clientId) throws IOException {
        log.info(">>> Unregister '{}'", clientId);
        var emitter = clients.remove(clientId);
        if (emitter != null) {
            emitter.send(SseEmitter
                .event()
                .id(String.valueOf(System.currentTimeMillis()))
                .name(EVENT_COMPLETE)
                .data(""));
            emitter.complete();
        }
        return ResponseEntity.ok(OK);
    }

    @GetMapping("/register")
    public ResponseEntity<SseEmitter> registerClient(@RequestParam final String clientId) throws IOException {
        var emitter = new SseEmitter(THREE_HOURS);
        if (clients.containsKey(clientId)) {
            log.info(">>> {} was already registered. Completing the previous registration.", clientId);
            clients.get(clientId).complete();
        }
        emitter.onTimeout(() -> {
            log.warn(">>> '{}' emitter timed out", clientId);
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
        log.info(">>> Registered '{}'", clientId);
        emitter.send(SseEmitter
            .event()
            .id(String.valueOf(System.currentTimeMillis()))
            .name(EVENT_REGISTERED)
            .data(""));
        return ResponseEntity.ok(emitter);
    }

    @GetMapping("/update")
    public void update(@RequestParam final String reason) {
        log.info(">>> Got an update request '{}'", reason);
        clients.entrySet().forEach(entry -> {
            try {
                String clientId = entry.getKey();
                SseEmitter sseEmitter = entry.getValue();
                log.info(">>> Notifying '{}'", clientId);
                sseEmitter.send(SseEmitter
                    .event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name(EVENT_UPDATE)
                    .data(reason));
                notifierService.notifier(Optional.of(reason), Optional.of(sseTimeout));
            } catch (IOException e) {
                log.error(">>> Sending '{}' failed:", reason, e);
                throw new RuntimeException(e);
            }
        });
    }
}

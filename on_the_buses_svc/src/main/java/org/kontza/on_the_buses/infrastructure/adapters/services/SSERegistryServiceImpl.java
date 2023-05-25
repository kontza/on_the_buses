package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.domain.api.SSERegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class SSERegistryServiceImpl implements SSERegistryService {
    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_COMPLETE = "COMPLETE";
    public static final String OK = "OK";
    public static final long THREE_HOURS = 10800000L;
    private final Map<String, SseEmitter> clients = new HashMap<>();
    private final NotifierService notifierService;

    @Value("${on-the-buses-svc.sse-timeout}")
    private long sseTimeout;

    public SSERegistryServiceImpl(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @Override
    public String unregisterClient(String clientId) throws IOException {
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
        return OK;
    }

    @Override
    public SseEmitter registerClient(String clientId) throws IOException {
        var emitter = new SseEmitter(THREE_HOURS);
        if (clients.containsKey(clientId)) {
            log.info(">>> {} was already registered. Completing the previous registration.", clientId);
            clients.get(clientId).complete();
        }
        emitter.onTimeout(() -> log.warn(">>> '{}' emitter timed out", clientId));
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
        return emitter;
    }

    @Override
    public void update(String reason) {
        update(reason, true);
    }

    @Override
    public void update(final String reason, boolean propagate) {
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
            } catch (IOException e) {
                log.error(">>> Sending '{}' failed:", reason, e);
                throw new RuntimeException(e);
            }
        });
        if (propagate) {
            notifierService.notifier(Optional.of(reason), Optional.of(sseTimeout));
        }
    }
}

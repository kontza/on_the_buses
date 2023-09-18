package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.domain.api.SseHandlerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SseHandlerServiceImpl implements SseHandlerService {
    public static final String EVENT_COMPLETE = "COMPLETE";
    public static final String EVENT_HEARTBEAT = "HEARTBEAT";
    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String OK = "OK";
    public static final long INFINITE_TIME = -1L;
    private final Map<String, SseEmitter> clients = new HashMap<>();
    private final NotifierService notifierService;

    @Value("${on-the-buses-svc.sse-timeout}")
    private long sseTimeout;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);

    public SseHandlerServiceImpl(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @PostConstruct
    void setupTimer() {
        executorService.scheduleAtFixedRate(this::sendHeartbeat, 15, 15, TimeUnit.SECONDS);
    }

    private void sendHeartbeat() {
        log.info(">>> Sending heartbeat to {}", clients.keySet());
        clients.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter
                        .event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name(EVENT_HEARTBEAT));
            } catch (IOException e) {
                log.error(">>> Failed to send heartbeat to '{}', cleaning it away", clientId, e);
                tryRemoveClient(clientId);
            }
        });
    }

    private boolean tryRemoveClient(String clientId) {
        boolean retVal = false;
        if (clients.containsKey(clientId)) {
            clients.remove(clientId).complete();
            retVal = true;
            log.warn(">>> '{}' removed from clients", clientId);
        } else {
            log.warn(">>> '{}' not in clients list", clientId);
        }
        return retVal;
    }

    @Override
    public String unregisterClient(String clientId) throws IOException {
        log.info(">>> Unregister '{}'", clientId);
        if (clients.containsKey(clientId)) {
            var emitter = clients.get(clientId);
            emitter.send(SseEmitter
                    .event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name(EVENT_COMPLETE));
            tryRemoveClient(clientId);
        } else {
            log.warn(">>> '{}' not a registered client", clientId);
        }
        return OK;
    }

    @Override
    public SseEmitter registerClient(String clientId) throws IOException {
        var emitter = new SseEmitter(INFINITE_TIME);
        if (clients.containsKey(clientId)) {
            log.info(">>> '{}' was already registered. Completing the previous registration.", clientId);
            clients.get(clientId).complete();
        }
        emitter.onTimeout(() -> log.warn(">>> '{}' emitter timed out", clientId));
        emitter.onError(err -> {
            log.error(">>> '{}' emitter error:", clientId, err);
            tryRemoveClient(clientId);
        });
        emitter.onCompletion(() -> {
            log.info(">>> '{}' emitter completed", clientId);
            tryRemoveClient(clientId);
        });
        clients.put(clientId, emitter);
        log.info(">>> Registered '{}'", clientId);
        try {
            emitter.send(SseEmitter
                    .event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name(EVENT_REGISTERED));
        } catch (IOException e) {
            log.error(">>> Failed to ACK registration for '{}'", clientId);
            throw e;
        }
        return emitter;
    }

    @Override
    public void update(final String reason, boolean propagate) {
        log.info(">>> Got an update request '{}', should propagate? {}. Clients: {}", reason, propagate, clients.keySet());
        clients.forEach((clientId, sseEmitter) -> {
            try {
                log.info(">>> Notifying '{}'", clientId);
                sseEmitter.send(SseEmitter
                        .event()
                        .id(String.valueOf(System.currentTimeMillis()))
                        .name(EVENT_UPDATE)
                        .data(reason));
            } catch (IOException e) {
                log.error(">>> Sending '{}' to '{}' failed:", reason, clientId, e);
                tryRemoveClient(clientId);
            }
        });
        if (propagate) {
            notifierService.notifier(reason, sseTimeout);
        }
    }
}

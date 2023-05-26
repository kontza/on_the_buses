package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.domain.api.SSEHandlerService;
import org.kontza.on_the_buses.infrastructure.adapters.model.HeartbeatPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SSEHandlerServiceImpl implements SSEHandlerService {
    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_COMPLETE = "COMPLETE";
    public static final String OK = "OK";
    public static final long INFINITE_TIME = -1L;
    public static final long HEARTBEAT_LIMIT = 15;
    private final Map<String, SseEmitter> clients = new HashMap<>();
    private final Map<String, ZonedDateTime> heartbeats = new HashMap<>();
    private final NotifierService notifierService;
    private final Registration registration;

    @Value("${on-the-buses-svc.sse-timeout}")
    private long sseTimeout;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public SSEHandlerServiceImpl(NotifierService notifierService, Registration registration) {
        this.notifierService = notifierService;
        this.registration = registration;
    }

    @PostConstruct
    void setupTimer() {
        executorService.scheduleAtFixedRate(this::doClientCleanup, 30, 30, TimeUnit.SECONDS);
    }

    private boolean tryRemoveClient(String clientId) {
        boolean retVal = false;
        if (clients.containsKey(clientId)) {
            clients.remove(clientId);
            retVal = true;
            log.warn(">>> '{}' removed from clients", clientId);
        } else {
            log.warn(">>> '{}' not a registered client", clientId);
        }
        if (heartbeats.containsKey(clientId)) {
            heartbeats.remove(clientId);
            retVal = true;
            log.warn(">>> '{}' removed from heartbeats", clientId);
        } else {
            log.warn(">>> '{}' does not have a heartbeat", clientId);
        }
        return retVal;
    }

    private void doClientCleanup() {
        log.info(">>> Starting to prune clients list...");
        var limit = ZonedDateTime.now().minusSeconds(HEARTBEAT_LIMIT);
        heartbeats.forEach((clientId, heartbeatTime) -> {
            if (heartbeatTime.isBefore(limit)) {
                log.info(">>> '{}' not see for a while, removing it...", clientId);
                tryRemoveClient(clientId);
            }
        });
        log.info(">>> ... prune done");
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
        var emitter = new SseEmitter(INFINITE_TIME);
        if (clients.containsKey(clientId)) {
            log.info(">>> {} was already registered. Completing the previous registration.", clientId);
            clients.get(clientId).complete();
        }
        emitter.onTimeout(() -> log.warn(">>> '{}' emitter timed out", clientId));
        emitter.onError(err -> {
            log.error(">>> '{}' emitter error:", clientId, err);
            tryRemoveClient(clientId);
            emitter.complete();
        });
        emitter.onCompletion(() -> {
            log.info(">>> '{}' emitter completed", clientId);
            tryRemoveClient(clientId);
        });
        var chair = clients.isEmpty();
        clients.put(clientId, emitter);
        heartbeats.put(clientId, ZonedDateTime.now());
        log.info(">>> Registered '{}'", clientId);
        try {
            emitter.send(SseEmitter
                .event()
                .id(String.valueOf(System.currentTimeMillis()))
                .name(EVENT_REGISTERED)
                .data(new HeartbeatPayload(clientId, registration.getInstanceId(), chair)));
        } catch (IOException e) {
            log.error(">>> Failed to ACK registration for '{}'", clientId);
            throw e;
        }
        return emitter;
    }

    @Override
    public void update(String reason) {
        update(reason, true);
    }

    @Override
    public void update(final String reason, boolean propagate) {
        log.info(">>> Got an update request '{}', should propagate? {}", reason, propagate);
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
                if (!tryRemoveClient(clientId)) {
                    throw new RuntimeException(e);
                }
            }
        });
        if (propagate) {
            notifierService.notifier(reason, sseTimeout);
        }
    }

    @Override
    public void heartbeat(HeartbeatPayload payload) {
        if (registration.getInstanceId().equals(payload.getInstanceId())) {
            handleHeartbeat(payload);
        } else {
            notifierService.heartbeat(payload, sseTimeout);
        }
    }

    private void handleHeartbeat(HeartbeatPayload payload) {
        log.info(">>> payload = {}, clients = {}", payload, clients);
        if (clients.containsKey(payload.getClientId())) {
            log.info(">>> Updated '{}' hearbeat", payload.getClientId());
            heartbeats.put(payload.getClientId(), ZonedDateTime.now());
        } else {
            var msg = String.format("Client '%s' not registered with '%s'", payload.getClientId(), registration.getInstanceId());
            log.error(">>> {}", msg);
            throw new NoSuchElementException(msg);
        }
    }
}

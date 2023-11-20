package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.domain.api.TrackingHandlerService;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.kontza.on_the_buses.infrastructure.adapters.services.NotifierServiceImpl.SOME_ID;
import static org.kontza.on_the_buses.infrastructure.adapters.services.NotifierServiceImpl.S_AMACCOUNT_NAME;

@Component
@Slf4j
public class TrackingHandlerServiceImpl implements TrackingHandlerService {
    public static final String EVENT_TIMEOUT = "TIMEOUT";
    public static final String EVENT_ERROR = "ERROR";
    private static final String EVENT_UNREGISTER = "UNREGISTER";
    public static final String OK = "OK";
    private static final Long WAIT_TIMEOUT = 5000L;
    private final Map<String, DeferredResult<LightEvent>> clients = new HashMap<>();
    private final NotifierService notifierService;

    public TrackingHandlerServiceImpl(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    private void tryRemoveClient(String clientId) {
        if (clients.containsKey(clientId)) {
            var deferred = clients.remove(clientId);
            if (deferred != null) {
                log.warn(">>> '{}' removed from clients", clientId);
                deferred.setResult(new LightEvent(EVENT_UNREGISTER, SOME_ID, S_AMACCOUNT_NAME));
            }
        } else {
            log.warn(">>> '{}' not in clients list", clientId);
        }
        logClients();
    }

    private void logClients() {
        log.info(">>> Clients: {}", clients.keySet());
    }

    @Override
    public String unregisterClient(String clientId) throws IOException {
        log.info(">>> Unregister '{}'", clientId);
        if (clients.containsKey(clientId)) {
            tryRemoveClient(clientId);
        } else {
            log.warn(">>> '{}' not a registered client", clientId);
        }
        logClients();
        return OK;
    }

    @Override
    public DeferredResult<LightEvent> registerClient(String clientId) throws IOException {
        var deferred = new DeferredResult<LightEvent>(WAIT_TIMEOUT, () -> {
            log.error(">>> {}", EVENT_TIMEOUT);
            var le = new LightEvent(EVENT_TIMEOUT, SOME_ID, S_AMACCOUNT_NAME);
            return new DeferredResult<LightEvent>().setResult(le);
        });
        if (clients.containsKey(clientId)) {
            log.info(">>> '{}' was already registered.", clientId);
        }
        deferred.onTimeout(() -> {
            log.warn(">>> '{}' deferred result timed out", clientId);
            clients.get(clientId).setResult(new LightEvent(EVENT_TIMEOUT, SOME_ID, S_AMACCOUNT_NAME));
            tryRemoveClient(clientId);
        });
        deferred.onError(err -> {
            log.error(">>> '{}' deferred result error:", clientId, err);
            clients.get(clientId).setResult(new LightEvent(EVENT_ERROR, SOME_ID, S_AMACCOUNT_NAME));
            tryRemoveClient(clientId);
        });
        deferred.onCompletion(() -> {
            log.info(">>> '{}' deferred result completed", clientId);
            tryRemoveClient(clientId);
        });
        clients.put(clientId, deferred);
        log.info(">>> Registered '{}'", clientId);
        logClients();
        return deferred;
    }

    @Override
    public void update(final String reason, boolean propagate) {
        log.info(">>> Got an update request '{}', should propagate? {}. Clients: {}", reason, propagate, clients.keySet());
        clients.forEach((clientId, deferred) -> {
            var payload = new LightEvent(reason, SOME_ID, S_AMACCOUNT_NAME);
            log.info(">>> Notifying '{}' with '{}'", clientId, payload);
            deferred.setResult(payload);
        });
        logClients();
        if (propagate) {
            notifierService.notifier(reason, WAIT_TIMEOUT);
        }
    }
}

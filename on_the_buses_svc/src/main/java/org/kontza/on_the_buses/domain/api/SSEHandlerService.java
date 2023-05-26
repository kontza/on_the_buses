package org.kontza.on_the_buses.domain.api;

import org.kontza.on_the_buses.infrastructure.adapters.model.HeartbeatPayload;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface SSEHandlerService {
    String unregisterClient(final String clientId) throws IOException;

    SseEmitter registerClient(final String clientId) throws IOException;

    void update(final String reason);

    void update(final String reason, boolean propagate);

    void heartbeat(HeartbeatPayload payload);
}

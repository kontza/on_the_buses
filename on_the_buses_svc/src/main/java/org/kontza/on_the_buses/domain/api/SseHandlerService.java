package org.kontza.on_the_buses.domain.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

public interface SseHandlerService {
    String unregisterClient(final String clientId) throws IOException;

    SseEmitter registerClient(final String clientId) throws IOException;

    void update(final String reason, boolean propagate);
}

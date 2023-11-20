package org.kontza.on_the_buses.domain.api;

import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

public interface TrackingHandlerService {
    String unregisterClient(final String clientId) throws IOException;

    DeferredResult<LightEvent> registerClient(final String clientId) throws IOException;

    void update(final String reason, boolean propagate);
}

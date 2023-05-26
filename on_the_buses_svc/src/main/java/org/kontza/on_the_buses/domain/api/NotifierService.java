package org.kontza.on_the_buses.domain.api;

import org.kontza.on_the_buses.infrastructure.adapters.model.HeartbeatPayload;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

public interface NotifierService {
    String notifier(String message, long timeout);

    void heartbeat(HeartbeatPayload payload, long timeout);
}

package org.kontza.on_the_buses.infrastructure.adapters.bus;

import org.kontza.on_the_buses.infrastructure.adapters.model.NotifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotifyListener {
    private static final Logger logger = LoggerFactory.getLogger(NotifyListener.class);

    @EventListener
    public void eventListener(NotifyEvent event) {
        logger.info("Received an event: {}", event);
    }
}

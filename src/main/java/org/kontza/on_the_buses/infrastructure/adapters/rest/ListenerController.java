package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.kontza.on_the_buses.infrastructure.configuration.EventSourceConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("listener")
@Slf4j
public class ListenerController {
    private EventSourceConfiguration eventSourceConfiguration;

    public ListenerController(EventSourceConfiguration eventSourceConfiguration) {
        this.eventSourceConfiguration = eventSourceConfiguration;
    }

    @PostMapping()
    public ResponseEntity<String> listener(@RequestBody LightEvent le) {
        if (le.getEventSource().equals(eventSourceConfiguration.getEventSource())) {
            log.info(">>> Notification from self. This can be ignored.");
        } else {
            log.info(">>> Got a notification: {}", le);
        }
        return ResponseEntity.ok("OK");
    }
}

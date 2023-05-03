package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.kontza.on_the_buses.infrastructure.configuration.EventSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("notify")
@Slf4j
public class NotifierController {
    public static final String STREAM_OUT = "notifyStream";
    public static final String DEFAULT_MESSAGE = "TRIGGERED!";
    private StreamBridge streamBridge;
    private EventSourceConfiguration eventSourceConfiguration;

    public NotifierController(StreamBridge streamBridge, EventSourceConfiguration eventSourceConfiguration) {
        this.streamBridge = streamBridge;
        this.eventSourceConfiguration = eventSourceConfiguration;
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam Optional<String> message) {
        var pojo = new LightEvent(eventSourceConfiguration.getEventSource(), message.orElse(DEFAULT_MESSAGE));
        streamBridge.send(STREAM_OUT, pojo);
        return ResponseEntity.ok("OK");
    }
}
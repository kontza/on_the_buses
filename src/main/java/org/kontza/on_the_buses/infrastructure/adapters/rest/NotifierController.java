package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
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
    public static final String STREAM_OUT = "notifyStream-out-0";
    public static final String DEFAULT_MESSAGE = "TRIGGERED!";
    private StreamBridge streamBridge;
//    private ApplicationJsonMessageMarshallingConverter converter;

    public NotifierController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam Optional<String> message) {
        var pojo = new LightEvent(message.orElse(DEFAULT_MESSAGE));
        log.info(">>> Calling notifyListener with '{}'", pojo);
        streamBridge.send(STREAM_OUT, pojo, MediaType.APPLICATION_JSON);
        return ResponseEntity.ok("OK");
    }
}
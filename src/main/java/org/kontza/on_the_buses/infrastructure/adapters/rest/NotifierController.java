package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
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
    public static final String STREAM_OUT = "notifyStream-out-0";
    private StreamBridge streamBridge;

    public NotifierController(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam Optional<String> message) {
        var payload = message.orElse("TRIGGERED!");
        log.info(">>> Calling notifyListener with '{}'", payload);
        streamBridge.send(STREAM_OUT, payload);
        return ResponseEntity.ok("OK");
    }
}
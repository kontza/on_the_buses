package org.kontza.on_the_buses.infrastructure.adapters.rest;

import org.kontza.on_the_buses.infrastructure.adapters.model.NotifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("notify")
public class NotifierController {
    private static final Logger logger = LoggerFactory.getLogger(NotifierController.class);

    private String busId;
    private ApplicationContext applicationContext;

    public NotifierController(@Value("${spring.cloud.bus.id}") String busId, ApplicationContext applicationContext) {
        this.busId = busId;
        this.applicationContext = applicationContext;
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam Optional<String> message) {
        logger.info("Bus ID = {}", busId);
        final var event = new NotifyEvent(this, busId, message.orElse("TRIGGERED!"));
        logger.info("Event = {}", event);
        applicationContext.publishEvent(event);
        return ResponseEntity.ok("OK");
    }
}
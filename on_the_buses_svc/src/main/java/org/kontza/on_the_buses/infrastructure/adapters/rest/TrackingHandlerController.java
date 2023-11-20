package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.TrackingHandlerService;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

@RestController
@RequestMapping("/track")
@Slf4j
public class TrackingHandlerController {
    private final TrackingHandlerService trackingHandlerService;

    public TrackingHandlerController(TrackingHandlerService trackingHandlerService) {
        this.trackingHandlerService = trackingHandlerService;
    }

    @GetMapping("/unregister")
    public ResponseEntity<String> unregisterClient(@RequestParam final String clientId) throws IOException {
        return ResponseEntity.ok(trackingHandlerService.unregisterClient(clientId));
    }

    @GetMapping("/register")
    public DeferredResult<LightEvent> registerClient(@RequestParam final String clientId) throws IOException {
        return trackingHandlerService.registerClient(clientId);
    }

    @GetMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public void update(@RequestParam final String reason) {
        trackingHandlerService.update(reason, true);
    }
}

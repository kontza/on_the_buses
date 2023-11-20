package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.TrackingHandlerService;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("listener")
@Slf4j
public class ListenerController {
    private final TrackingHandlerService trackingHandlerService;

    public ListenerController(TrackingHandlerService trackingHandlerService) {
        this.trackingHandlerService = trackingHandlerService;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.OK)
    public void listener(@RequestBody LightEvent le) throws InterruptedException {
        Thread.sleep(5000);
        trackingHandlerService.update(le.getReason(), false);
    }
}

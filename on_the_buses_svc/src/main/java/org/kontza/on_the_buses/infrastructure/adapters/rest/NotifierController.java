package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("notify")
@Slf4j
public class NotifierController {
    private final NotifierService notifierService;

    public NotifierController(NotifierService notifierService) {
        this.notifierService = notifierService;
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam String message, @RequestParam long timeout) {
        return ResponseEntity.ok(notifierService.notifier(message, timeout));
    }
}
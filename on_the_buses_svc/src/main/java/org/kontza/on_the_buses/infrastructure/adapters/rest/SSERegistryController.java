package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.SSERegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SSERegistryController {
    private final SSERegistryService sseRegistryService;

    public SSERegistryController(SSERegistryService sseRegistryService) {
        this.sseRegistryService = sseRegistryService;
    }

    @GetMapping("/unregister")
    public ResponseEntity<String> unregisterClient(@RequestParam final String clientId) throws IOException {
        return ResponseEntity.ok(sseRegistryService.unregisterClient(clientId));
    }

    @GetMapping("/register")
    public ResponseEntity<SseEmitter> registerClient(@RequestParam final String clientId) throws IOException {
        return ResponseEntity.ok(sseRegistryService.registerClient(clientId));
    }

    @GetMapping("/update")
    public void update(@RequestParam final String reason) {
        sseRegistryService.update(reason);
    }
}

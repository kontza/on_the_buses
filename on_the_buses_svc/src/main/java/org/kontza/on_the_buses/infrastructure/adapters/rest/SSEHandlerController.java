package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.SSEHandlerService;
import org.kontza.on_the_buses.infrastructure.adapters.model.HeartbeatPayload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/sse")
@Slf4j
public class SSEHandlerController {
    private final SSEHandlerService sseHandlerService;

    public SSEHandlerController(SSEHandlerService sseHandlerService) {
        this.sseHandlerService = sseHandlerService;
    }

    @GetMapping("/unregister")
    public ResponseEntity<String> unregisterClient(@RequestParam final String clientId) throws IOException {
        return ResponseEntity.ok(sseHandlerService.unregisterClient(clientId));
    }

    @GetMapping("/register")
    public ResponseEntity<SseEmitter> registerClient(@RequestParam final String clientId) throws IOException {
        return ResponseEntity.ok(sseHandlerService.registerClient(clientId));
    }

    @GetMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    public void update(@RequestParam final String reason) {
        sseHandlerService.update(reason);
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.OK)
    public void heartbeat(@RequestBody final HeartbeatPayload payload) {
        sseHandlerService.heartbeat(payload);
    }
}

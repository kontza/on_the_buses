package org.kontza.on_the_buses.infrastructure.adapters.rest;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.kontza.on_the_buses.infrastructure.configuration.EventSourceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("notify")
@Slf4j
public class NotifierController {
    public static final String DEFAULT_MESSAGE = "TRIGGERED!";
    public static final String S_AMACCOUNT_NAME = "v:AD_DOMAIN\\sAMAccountName";
    private static final Long DEFAULT_TIMEOUT = 1000l;
    private EventSourceConfiguration eventSourceConfiguration;
    private DiscoveryClient discoveryClient;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${server.port}")
    private int appPort;
    private WebClient webClient;

    public NotifierController(EventSourceConfiguration eventSourceConfiguration, DiscoveryClient discoveryClient) {
        this.eventSourceConfiguration = eventSourceConfiguration;
        this.discoveryClient = discoveryClient;
        webClient = WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private Mono<String> notify(String uri, LightEvent le) {
        log.info(">>> Calling {}/listener with {}", uri, le);
        return webClient
            .post()
            .uri(uri + "/listener")
            .body(Mono.just(le), LightEvent.class)
            .retrieve()
            .bodyToMono(String.class);
    }

    @GetMapping()
    public ResponseEntity<String> notifier(@RequestParam Optional<String> message, @RequestParam Optional<Long> timeout) {
        log.info(">>> Using a timeout value of {}", timeout.orElse(DEFAULT_TIMEOUT));
        var le = new LightEvent(
            eventSourceConfiguration.getEventSource(),
            message.orElse(DEFAULT_MESSAGE),
            42l,
            S_AMACCOUNT_NAME
        );
        var instances = discoveryClient.getInstances(appName);
        List<Mono<String>> responses = new ArrayList<>();
        instances.forEach(serviceInstance -> responses.add(notify(String.valueOf(serviceInstance.getUri()), le)));
        log.info(">>> Waiting...");
        var merge = Flux.merge(responses).timeout(Duration.ofMillis(timeout.orElse(DEFAULT_TIMEOUT)));
        merge.doOnComplete(() -> log.info(">>> onComplete"))
            .doOnError(e -> log.error(">>> onError", e))
            .blockLast();
        return ResponseEntity.ok("OK");
    }
}
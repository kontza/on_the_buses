package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class NotifierServiceImpl implements NotifierService {
    public static final String DEFAULT_MESSAGE = "TRIGGERED!";
    public static final String S_AMACCOUNT_NAME = "v:AD_DOMAIN\\sAMAccountName";
    private static final Long DEFAULT_TIMEOUT = 1000l;
    private DiscoveryClient discoveryClient;
    @Value("${spring.application.name}")
    private String appName;
    @Value("${server.port}")
    private int appPort;
    private WebClient webClient;

    public NotifierServiceImpl(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        webClient = WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private Mono<Void> notify(ServiceInstance serviceInstance, LightEvent le) {
        log.info(">>> Calling {}/listener with {}", serviceInstance, le);
        return webClient
            .post()
            .uri(serviceInstance.getUri() + "/listener")
            .body(Mono.just(le), LightEvent.class)
            .retrieve()
            .bodyToMono(Void.class);
    }

    @Override
    public ResponseEntity<String> notifier(Optional<String> message, Optional<Long> timeout) {
        log.info(">>> Using a timeout value of {}", timeout.orElse(DEFAULT_TIMEOUT));
        var le = new LightEvent(
            message.orElse(DEFAULT_MESSAGE),
            42l,
            S_AMACCOUNT_NAME
        );
        var instances = discoveryClient.getInstances(appName);
        List<Mono<Void>> responses = new ArrayList<>();
        var instanceId = String.format("%s-%d", appName, appPort);
        instances.forEach(serviceInstance -> {
            if (serviceInstance.getInstanceId().equals(instanceId)) {
                log.info(">>> Not going to notify self.");
            } else {
                responses.add(notify(serviceInstance, le));
            }
        });
        log.info(">>> Waiting...");
        var merge = Flux.merge(responses).timeout(Duration.ofMillis(timeout.orElse(DEFAULT_TIMEOUT)));
        merge.doOnComplete(() -> log.info(">>> onComplete"))
            .doOnError(e -> log.error(">>> onError", e))
            .blockLast();
        return ResponseEntity.ok("OK");
    }
}

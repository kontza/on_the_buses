package org.kontza.on_the_buses.infrastructure.adapters.services;

import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.domain.api.NotifierService;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.kontza.on_the_buses.infrastructure.adapters.services.SseHandlerServiceImpl.OK;

@Component
@Slf4j
public class NotifierServiceImpl implements NotifierService {
    public static final String DEFAULT_MESSAGE = "TRIGGERED!";
    public static final String S_AMACCOUNT_NAME = "v:AD_DOMAIN\\sAMAccountName";
    private static final Long DEFAULT_TIMEOUT = 1000L;
    private final DiscoveryClient discoveryClient;
    private final Registration registration;
    private final WebClient webClient;

    public NotifierServiceImpl(DiscoveryClient discoveryClient, Registration registration) {
        this.discoveryClient = discoveryClient;
        this.registration = registration;
        webClient = WebClient
            .builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private Mono<Void> notify(ServiceInstance serviceInstance, LightEvent le) {
        log.info(">>> Calling {}/listener with {}", serviceInstance.getUri(), le);
        return webClient
            .post()
            .uri(serviceInstance.getUri() + "/listener")
            .body(Mono.just(le), LightEvent.class)
            .retrieve()
            .bodyToMono(Void.class);
    }

    @Override
    public String notifier(String message, long timeout) {
        if (timeout < 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        if (message == null) {
            message = DEFAULT_MESSAGE;
        }
        log.info(">>> Using a timeout value of {}", timeout);
        var le = new LightEvent(
            message,
            42l,
            S_AMACCOUNT_NAME
        );
        var instances = discoveryClient.getInstances(registration.getServiceId());
        List<Mono<Void>> responses = new ArrayList<>();
        log.info(">>> Self: {}", registration.getUri());
        instances.forEach(serviceInstance -> {
            if ((serviceInstance.getHost().equals(registration.getHost()))
                && (serviceInstance.getPort() == registration.getPort())) {
                log.info(">>> Not going to notify self.");
            } else {
                log.info(">>> Other: {}", serviceInstance.getUri());
                responses.add(notify(serviceInstance, le));
            }
        });
        log.info(">>> Waiting...");
        var merge = Flux.merge(responses);
        merge.doOnComplete(() -> log.info(">>> notify call onComplete"))
            .doOnError(e -> log.error(">>> notify call onError", e))
            .subscribe();
        return OK;
    }
}

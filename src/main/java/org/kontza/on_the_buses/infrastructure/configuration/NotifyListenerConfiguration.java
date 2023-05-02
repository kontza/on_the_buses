package org.kontza.on_the_buses.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class NotifyListenerConfiguration {
    @Bean
    public Consumer<Object> notifyListener() {
        log.info(">>> notifyListener preflight");
        return msg -> log.info(">>> notifyListener Consumed {}", msg);
    }
}

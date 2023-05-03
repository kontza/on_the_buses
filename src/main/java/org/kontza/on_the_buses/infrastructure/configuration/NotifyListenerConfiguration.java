package org.kontza.on_the_buses.infrastructure.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.kontza.on_the_buses.infrastructure.adapters.model.LightEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;


@Configuration
@Slf4j
public class NotifyListenerConfiguration {
    private ObjectMapper objectMapper;
    private EventSourceConfiguration eventSourceConfiguration;

    public NotifyListenerConfiguration(ObjectMapper objectMapper, EventSourceConfiguration eventSourceConfiguration) {
        this.objectMapper = objectMapper;
        this.eventSourceConfiguration = eventSourceConfiguration;
    }

    @Bean
    public Consumer<?> notifyListener() {
        return msg -> {
            final byte[] bytes;
            try {
                bytes = objectMapper.readValue((String) msg, byte[].class);
            } catch (JsonProcessingException e) {
                log.error("Failed to read a byte array from '{}'", msg, e);
                throw new RuntimeException(e);
            }
            var asString = new String(bytes, StandardCharsets.UTF_8);
            final LightEvent le;
            try {
                le = objectMapper.readValue(asString, LightEvent.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to map a LightEvent from '{}'", asString, e);
                throw new RuntimeException(e);
            }
            if (le.getEventSource().equals(eventSourceConfiguration.getEventSource())) {
                log.info(">>> Self-sent message '{}'. Do not process it further.", le);
            } else {
                log.info(">>> Start processing '{}'", le);
            }
        };
    }
}

package org.kontza.on_the_buses.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
public class EventSourceConfiguration {
    private String eventSource;

    public EventSourceConfiguration(@Value("${spring.application.name}-${server.port}") String eventSource) {
        this.eventSource = eventSource;
    }
}

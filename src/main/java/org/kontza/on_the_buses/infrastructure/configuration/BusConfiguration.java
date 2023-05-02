package org.kontza.on_the_buses.infrastructure.configuration;

import org.springframework.cloud.bus.jackson.RemoteApplicationEventScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@RemoteApplicationEventScan(basePackages = "org.kontza.on_the_buses.infrastructure.adapters.model")
public class BusConfiguration {
}

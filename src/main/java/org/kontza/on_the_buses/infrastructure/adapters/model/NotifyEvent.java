package org.kontza.on_the_buses.infrastructure.adapters.model;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@ToString
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class NotifyEvent extends RemoteApplicationEvent {
    private String payload;

    public NotifyEvent(Object source, String originService, String payload) {
        super(source, originService);
        this.payload = payload;
    }

    public NotifyEvent(Object source, String originService, Destination destination, String payload) {
        super(source, originService, destination);
        this.payload = payload;
    }
}

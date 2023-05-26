package org.kontza.on_the_buses.infrastructure.adapters.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatPayload {
    private String clientId;
    private String instanceId;
    private boolean chair;
}

package org.kontza.on_the_buses.domain.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

public interface NotifierService {
    ResponseEntity<String> notifier(@RequestParam Optional<String> message, @RequestParam Optional<Long> timeout);
}

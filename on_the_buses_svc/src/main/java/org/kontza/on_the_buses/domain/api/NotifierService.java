package org.kontza.on_the_buses.domain.api;

public interface NotifierService {
    String notifier(String message, long timeout);
}

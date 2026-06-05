package org.example.clinic.server.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class AppointmentEventBus {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventBus.class);

    private final List<AppointmentEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(AppointmentEventListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(AppointmentEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(AppointmentEvent event) {
        for (AppointmentEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ex) {
                log.error("Listener {} failed for event {}",
                        listener.getClass().getSimpleName(), event.type(), ex);
            }
        }
    }
}

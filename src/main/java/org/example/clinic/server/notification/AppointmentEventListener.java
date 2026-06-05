package org.example.clinic.server.notification;


@FunctionalInterface
public interface AppointmentEventListener {
    void onEvent(AppointmentEvent event);
}

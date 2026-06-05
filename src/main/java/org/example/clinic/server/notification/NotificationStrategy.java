package org.example.clinic.server.notification;


@FunctionalInterface
public interface NotificationStrategy {

    void send(NotificationMessage message) throws NotificationException;
}

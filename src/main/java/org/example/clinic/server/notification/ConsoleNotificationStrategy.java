package org.example.clinic.server.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConsoleNotificationStrategy implements NotificationStrategy {

    private static final Logger log = LoggerFactory.getLogger(ConsoleNotificationStrategy.class);

    @Override
    public void send(NotificationMessage message) {
        log.info("[CONSOLE-NOTIFY] to={} <{}> | subject='{}'\n{}",
                message.getRecipientName(),
                message.getRecipientEmail(),
                message.getSubject(),
                message.getBody());
    }
}

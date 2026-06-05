package org.example.clinic.server.notification;


public class NotificationMessage {

    private final String recipientEmail;
    private final String recipientName;
    private final String subject;
    private final String body;

    public NotificationMessage(String recipientEmail, String recipientName,
                               String subject, String body) {
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.body = body;
    }

    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientName() { return recipientName; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
}

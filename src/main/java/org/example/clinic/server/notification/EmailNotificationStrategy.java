package org.example.clinic.server.notification;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Properties;


public class EmailNotificationStrategy implements NotificationStrategy {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationStrategy.class);

    private final Session session;
    private final String fromAddress;
    private final String fromName;

    public EmailNotificationStrategy(String smtpHost,
                                     int smtpPort,
                                     boolean startTls,
                                     boolean auth,
                                     String username,
                                     String password,
                                     String fromAddress,
                                     String fromName) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        Authenticator authenticator = null;
        if (auth) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }

        this.session = Session.getInstance(props, authenticator);
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void send(NotificationMessage message) throws NotificationException {
        try {
            MimeMessage mime = new MimeMessage(session);
            mime.setFrom(buildFrom());
            mime.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(message.getRecipientEmail(), message.getRecipientName(), "UTF-8"));
            mime.setSubject(message.getSubject(), "UTF-8");
            mime.setText(message.getBody(), "UTF-8");

            Transport.send(mime);
            log.info("Email sent to {} (subject='{}')",
                    message.getRecipientEmail(), message.getSubject());
        } catch (MessagingException | UnsupportedEncodingException ex) {
            throw new NotificationException(
                    "Failed to send email: " + ex.getMessage(), ex);
        }
    }

    private InternetAddress buildFrom() throws AddressException, UnsupportedEncodingException {
        return fromName == null
                ? new InternetAddress(fromAddress)
                : new InternetAddress(fromAddress, fromName, "UTF-8");
    }
}

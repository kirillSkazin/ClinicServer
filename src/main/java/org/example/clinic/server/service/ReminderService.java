package org.example.clinic.server.service;

import org.example.clinic.server.dao.ReminderDao;
import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.Reminder;
import org.example.clinic.server.notification.NotificationException;
import org.example.clinic.server.notification.NotificationMessage;
import org.example.clinic.server.notification.NotificationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ReminderService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int MAX_ATTEMPTS = 3;
    private static final int SCAN_BATCH = 50;

    private final ReminderDao reminderDao;
    private final NotificationStrategy notificationStrategy;
    private final long scanPeriodSeconds;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workers;

    public ReminderService(ReminderDao reminderDao,
                           NotificationStrategy notificationStrategy,
                           long scanPeriodSeconds,
                           int workerThreads) {
        this.reminderDao = reminderDao;
        this.notificationStrategy = notificationStrategy;
        this.scanPeriodSeconds = scanPeriodSeconds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "reminder-scanner");
            t.setDaemon(true);
            return t;
        });
        this.workers = Executors.newFixedThreadPool(Math.max(1, workerThreads), r -> {
            Thread t = new Thread(r, "reminder-worker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        log.info("Reminder scheduler started, scan period = {}s", scanPeriodSeconds);
        scheduler.scheduleAtFixedRate(this::scanAndDispatch,
                scanPeriodSeconds, scanPeriodSeconds, TimeUnit.SECONDS);
    }

    void scanAndDispatch() {
        try {
            List<Reminder> due = reminderDao.findDueForSending(LocalDateTime.now(), SCAN_BATCH);
            if (due.isEmpty()) {
                return;
            }
            log.debug("Found {} reminders to send", due.size());
            for (Reminder r : due) {
                workers.submit(() -> processOne(r.getId()));
            }
        } catch (RuntimeException ex) {
            log.error("Failed to scan reminders", ex);
        }
    }

    private void processOne(Long reminderId) {
        Reminder r = reminderDao.findById(reminderId).orElse(null);
        if (r == null || r.getStatus() != Reminder.Status.PENDING) {
            return;
        }
        Appointment a = r.getAppointment();
        if (a == null || a.getStatus() == AppointmentStatus.CANCELLED) {
            r.setStatus(Reminder.Status.CANCELLED);
            reminderDao.update(r);
            return;
        }
        try {
            NotificationMessage msg = buildMessage(a);
            notificationStrategy.send(msg);
            r.setStatus(Reminder.Status.SENT);
            r.setSentAt(LocalDateTime.now());
            r.setLastError(null);
        } catch (NotificationException ex) {
            r.setAttempts(r.getAttempts() + 1);
            r.setLastError(ex.getMessage());
            if (r.getAttempts() >= MAX_ATTEMPTS) {
                r.setStatus(Reminder.Status.FAILED);
                log.warn("Reminder #{} permanently failed after {} attempts: {}",
                        r.getId(), r.getAttempts(), ex.getMessage());
            } else {
                LocalDateTime nextTry = LocalDateTime.now().plusMinutes(5L * r.getAttempts());
                r.setSendAt(nextTry);
                log.info("Reminder #{} attempt {} failed, retry at {}",
                        r.getId(), r.getAttempts(), nextTry);
            }
        }
        reminderDao.update(r);
    }

    private NotificationMessage buildMessage(Appointment a) {
        String email = a.getPatient().getUser().getEmail();
        String name = a.getPatient().getUser().getFullName();
        String subject = "Напоминание о приёме у врача";
        String body = """
                Здравствуйте, %s!
                
                Напоминаем, что %s у Вас запланирован приём.
                Врач: %s (%s)
                Кабинет: %s
                Длительность: %d минут
                
                Пожалуйста, не опаздывайте. Если планы изменились —
                отмените запись через приложение клиники.
                
                С уважением,
                Регистратура клиники
                """.formatted(
                name,
                a.getStartsAt().format(DATE_FMT),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getSpecialization().getName(),
                a.getDoctor().getRoomNumber() == null ? "—" : a.getDoctor().getRoomNumber(),
                a.getDurationMinutes());
        return new NotificationMessage(email, name, subject, body);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException ie) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Reminder scheduler stopped");
    }
}

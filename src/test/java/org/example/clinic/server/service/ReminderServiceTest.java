package org.example.clinic.server.service;

import org.example.clinic.server.dao.ReminderDao;
import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.Doctor;
import org.example.clinic.server.model.Patient;
import org.example.clinic.server.model.Reminder;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.Specialization;
import org.example.clinic.server.model.User;
import org.example.clinic.server.notification.NotificationException;
import org.example.clinic.server.notification.NotificationStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock ReminderDao reminderDao;
    @Mock NotificationStrategy notificationStrategy;

    private Reminder buildReminder(AppointmentStatus apptStatus, Reminder.Status status) {
        Specialization s = new Specialization("Терапевт", null);
        s.setId(1L);
        User docUser = User.builder().username("doc").passwordHash("h")
                .email("d@d.com").fullName("Dr").role(Role.DOCTOR).build();
        docUser.setId(2L);
        Doctor d = new Doctor(docUser, s, "100", 0, "");
        d.setId(2L);

        User patUser = User.builder().username("p").passwordHash("h")
                .email("p@p.com").fullName("Patient").role(Role.PATIENT).build();
        patUser.setId(3L);
        Patient p = new Patient(patUser, null, null, null);
        p.setId(3L);

        Appointment a = Appointment.builder()
                .doctor(d).patient(p)
                .startsAt(LocalDateTime.now().plusDays(1))
                .status(apptStatus).durationMinutes(30).build();
        a.setId(10L);

        Reminder r = new Reminder(a, LocalDateTime.now().minusMinutes(1), Reminder.Channel.EMAIL);
        r.setId(99L);
        r.setStatus(status);
        return r;
    }

    @Test
    void sentReminderIsMarkedAsSent() throws Exception {
        Reminder r = buildReminder(AppointmentStatus.PLANNED, Reminder.Status.PENDING);
        when(reminderDao.findDueForSending(any(), anyInt())).thenReturn(List.of(r));
        when(reminderDao.findById(99L)).thenReturn(Optional.of(r));

        try (ReminderService svc = new ReminderService(
                reminderDao, notificationStrategy, 60, 1)) {
            svc.scanAndDispatch();
            
            Thread.sleep(300);
        }

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderDao, times(1)).update(captor.capture());
        Reminder updated = captor.getValue();
        assertEquals(Reminder.Status.SENT, updated.getStatus());
        assertNotNull(updated.getSentAt());
        verify(notificationStrategy, times(1)).send(any());
    }

    @Test
    void cancelledAppointmentSkipsSendingAndCancelsReminder() throws Exception {
        Reminder r = buildReminder(AppointmentStatus.CANCELLED, Reminder.Status.PENDING);
        when(reminderDao.findDueForSending(any(), anyInt())).thenReturn(List.of(r));
        when(reminderDao.findById(99L)).thenReturn(Optional.of(r));

        try (ReminderService svc = new ReminderService(
                reminderDao, notificationStrategy, 60, 1)) {
            svc.scanAndDispatch();
            Thread.sleep(300);
        }

        verify(notificationStrategy, times(0)).send(any());
        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderDao, times(1)).update(captor.capture());
        assertEquals(Reminder.Status.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void failedSendIncrementsAttempts() throws Exception {
        Reminder r = buildReminder(AppointmentStatus.PLANNED, Reminder.Status.PENDING);
        when(reminderDao.findDueForSending(any(), anyInt())).thenReturn(List.of(r));
        when(reminderDao.findById(99L)).thenReturn(Optional.of(r));
        doThrow(new NotificationException("smtp down"))
                .when(notificationStrategy).send(any());

        try (ReminderService svc = new ReminderService(
                reminderDao, notificationStrategy, 60, 1)) {
            svc.scanAndDispatch();
            Thread.sleep(300);
        }

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderDao, times(1)).update(captor.capture());
        Reminder updated = captor.getValue();
        assertEquals(1, updated.getAttempts());
        assertEquals(Reminder.Status.PENDING, updated.getStatus());
        assertEquals("smtp down", updated.getLastError());
    }
}

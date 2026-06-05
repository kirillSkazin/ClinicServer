package org.example.clinic.server.service;

import org.example.clinic.server.dao.AppointmentDao;
import org.example.clinic.server.dao.DoctorDao;
import org.example.clinic.server.dao.PatientDao;
import org.example.clinic.server.dao.ReminderDao;
import org.example.clinic.server.dto.AppointmentView;
import org.example.clinic.server.dto.BookAppointmentRequest;
import org.example.clinic.server.exception.ConflictException;
import org.example.clinic.server.exception.ForbiddenException;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.Doctor;
import org.example.clinic.server.model.Patient;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.Specialization;
import org.example.clinic.server.model.User;
import org.example.clinic.server.notification.AppointmentEvent;
import org.example.clinic.server.notification.AppointmentEventBus;
import org.example.clinic.server.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentDao appointmentDao;
    @Mock DoctorDao doctorDao;
    @Mock PatientDao patientDao;
    @Mock ReminderDao reminderDao;
    @Spy AppointmentEventBus eventBus;

    @InjectMocks AppointmentService service;

    private Doctor doctor;
    private Patient patient;
    private AuthPrincipal patientPrincipal;
    private AuthPrincipal doctorPrincipal;

    @BeforeAll
    static void initConfig() {
        
        
        try {
            Class<?> cls = Class.forName("org.example.clinic.server.config.AppConfig");
            Field instance = cls.getDeclaredField("instance");
            instance.setAccessible(true);
            java.lang.reflect.Constructor<?> ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object cfg = ctor.newInstance();
            Field values = cls.getDeclaredField("values");
            values.setAccessible(true);
            Map<String, String> map = new HashMap<>();
            map.put("reminder.enabled", "true");
            map.put("reminder.lead-hours", "24");
            @SuppressWarnings("unchecked")
            Map<String, String> backing = (Map<String, String>) values.get(cfg);
            backing.putAll(map);
            instance.set(null, cfg);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @BeforeEach
    void setup() {
        Specialization spec = new Specialization("Терапевт", null);
        spec.setId(10L);

        User docUser = User.builder().username("doc").passwordHash("h")
                .email("d@d.com").fullName("Dr. House").role(Role.DOCTOR).build();
        docUser.setId(2L);
        doctor = new Doctor(docUser, spec, "101", 5, "");
        doctor.setId(20L);

        User patUser = User.builder().username("pat").passwordHash("h")
                .email("p@p.com").fullName("Пациент").role(Role.PATIENT).build();
        patUser.setId(3L);
        patient = new Patient(patUser, null, null, null);
        patient.setId(30L);

        patientPrincipal = new AuthPrincipal(3L, "pat", Role.PATIENT);
        doctorPrincipal = new AuthPrincipal(2L, "doc", Role.DOCTOR);
    }

    @Test
    void bookCreatesAppointmentWhenSlotIsFree() {
        when(doctorDao.findById(20L)).thenReturn(Optional.of(doctor));
        when(patientDao.findByUserId(3L)).thenReturn(Optional.of(patient));
        when(appointmentDao.hasDoctorOverlap(eq(20L), any(), anyInt(), eq(null))).thenReturn(false);
        when(appointmentDao.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(100L);
            return a;
        });

        BookAppointmentRequest req = new BookAppointmentRequest();
        req.setDoctorId(20L);
        req.setStartsAt(LocalDateTime.now().plusDays(1));
        req.setDurationMinutes(30);
        req.setComplaint("Кашель");

        AppointmentView view = service.book(patientPrincipal, req);
        assertEquals(100L, view.getId());
        assertEquals(AppointmentStatus.PLANNED, view.getStatus());

        ArgumentCaptor<AppointmentEvent> ev = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(eventBus).publish(ev.capture());
        assertEquals(AppointmentEvent.Type.BOOKED, ev.getValue().type());
        verify(reminderDao, times(1)).save(any());
    }

    @Test
    void bookFailsWhenSlotOverlaps() {
        when(doctorDao.findById(20L)).thenReturn(Optional.of(doctor));
        when(patientDao.findByUserId(3L)).thenReturn(Optional.of(patient));
        when(appointmentDao.hasDoctorOverlap(eq(20L), any(), anyInt(), eq(null))).thenReturn(true);

        BookAppointmentRequest req = new BookAppointmentRequest();
        req.setDoctorId(20L);
        req.setStartsAt(LocalDateTime.now().plusDays(1));

        assertThrows(ConflictException.class, () -> service.book(patientPrincipal, req));
        verify(appointmentDao, never()).save(any());
    }

    @Test
    void bookFailsForPastDate() {
        when(doctorDao.findById(20L)).thenReturn(Optional.of(doctor));
        when(patientDao.findByUserId(3L)).thenReturn(Optional.of(patient));

        BookAppointmentRequest req = new BookAppointmentRequest();
        req.setDoctorId(20L);
        req.setStartsAt(LocalDateTime.now().minusDays(1));

        assertThrows(ValidationException.class, () -> service.book(patientPrincipal, req));
    }

    @Test
    void bookFailsForUnknownDoctor() {
        when(doctorDao.findById(99L)).thenReturn(Optional.empty());

        BookAppointmentRequest req = new BookAppointmentRequest();
        req.setDoctorId(99L);
        req.setStartsAt(LocalDateTime.now().plusDays(1));

        assertThrows(NotFoundException.class, () -> service.book(patientPrincipal, req));
    }

    @Test
    void cancelChangesStatusAndCancelsReminders() {
        Appointment a = Appointment.builder()
                .doctor(doctor).patient(patient)
                .startsAt(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.PLANNED)
                .build();
        a.setId(50L);
        when(appointmentDao.findById(50L)).thenReturn(Optional.of(a));
        when(appointmentDao.update(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentView view = service.cancel(patientPrincipal, 50L);
        assertEquals(AppointmentStatus.CANCELLED, view.getStatus());
        verify(reminderDao).cancelByAppointment(50L);
    }

    @Test
    void cancelDeniedForOtherPatient() {
        AuthPrincipal otherPatient = new AuthPrincipal(999L, "other", Role.PATIENT);
        Appointment a = Appointment.builder()
                .doctor(doctor).patient(patient)
                .startsAt(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.PLANNED)
                .build();
        a.setId(50L);
        when(appointmentDao.findById(50L)).thenReturn(Optional.of(a));

        assertThrows(ForbiddenException.class, () -> service.cancel(otherPatient, 50L));
    }

    @Test
    void confirmRequiresOwnDoctor() {
        Appointment a = Appointment.builder()
                .doctor(doctor).patient(patient)
                .startsAt(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.PLANNED)
                .build();
        a.setId(50L);
        when(appointmentDao.findById(50L)).thenReturn(Optional.of(a));
        when(appointmentDao.update(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentView view = service.confirm(doctorPrincipal, 50L);
        assertEquals(AppointmentStatus.CONFIRMED, view.getStatus());
    }

    @Test
    void completeFailsForCancelled() {
        Appointment a = Appointment.builder()
                .doctor(doctor).patient(patient)
                .startsAt(LocalDateTime.now().plusDays(1))
                .status(AppointmentStatus.CANCELLED)
                .build();
        a.setId(60L);
        when(appointmentDao.findById(60L)).thenReturn(Optional.of(a));

        assertThrows(ConflictException.class,
                () -> service.complete(doctorPrincipal, 60L, "ok"));
    }
}

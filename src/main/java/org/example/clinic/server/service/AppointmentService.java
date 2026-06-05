package org.example.clinic.server.service;

import org.example.clinic.server.config.AppConfig;
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
import org.example.clinic.server.model.Reminder;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.notification.AppointmentEvent;
import org.example.clinic.server.notification.AppointmentEventBus;
import org.example.clinic.server.security.AuthPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;


public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final PatientDao patientDao;
    private final ReminderDao reminderDao;
    private final AppointmentEventBus eventBus;

    public AppointmentService(AppointmentDao appointmentDao,
                              DoctorDao doctorDao,
                              PatientDao patientDao,
                              ReminderDao reminderDao,
                              AppointmentEventBus eventBus) {
        this.appointmentDao = appointmentDao;
        this.doctorDao = doctorDao;
        this.patientDao = patientDao;
        this.reminderDao = reminderDao;
        this.eventBus = eventBus;
    }

    public AppointmentView book(AuthPrincipal principal, BookAppointmentRequest req) {
        validate(req);
        Doctor doctor = doctorDao.findById(req.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Врач не найден: " + req.getDoctorId()));
        Patient patient = patientDao.findByUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("Профиль пациента не найден"));

        if (req.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Нельзя записаться на прошедшее время");
        }
        int duration = req.getDurationMinutes() == null ? 30 : req.getDurationMinutes();

        if (appointmentDao.hasDoctorOverlap(doctor.getId(), req.getStartsAt(), duration, null)) {
            throw new ConflictException("Выбранное время уже занято");
        }

        Appointment a = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .startsAt(req.getStartsAt())
                .durationMinutes(duration)
                .complaint(req.getComplaint())
                .status(AppointmentStatus.PLANNED)
                .build();
        Appointment saved = appointmentDao.save(a);

        scheduleReminder(saved);
        eventBus.publish(new AppointmentEvent(AppointmentEvent.Type.BOOKED, saved));
        log.info("Patient {} booked appointment #{} with doctor {} at {}",
                principal.userId(), saved.getId(), doctor.getId(), saved.getStartsAt());
        return AppointmentView.from(saved);
    }

    public AppointmentView cancel(AuthPrincipal principal, Long appointmentId) {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена: " + appointmentId));
        ensureCanModify(principal, a);
        if (a.getStatus() == AppointmentStatus.CANCELLED
                || a.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Нельзя отменить запись в статусе " + a.getStatus());
        }
        a.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentDao.update(a);
        reminderDao.cancelByAppointment(updated.getId());
        eventBus.publish(new AppointmentEvent(AppointmentEvent.Type.CANCELLED, updated));
        return AppointmentView.from(updated);
    }

    public AppointmentView confirm(AuthPrincipal principal, Long appointmentId) {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена: " + appointmentId));
        ensureDoctorOrAdmin(principal, a);
        if (a.getStatus() != AppointmentStatus.PLANNED) {
            throw new ConflictException("Можно подтвердить только запланированный приём");
        }
        a.setStatus(AppointmentStatus.CONFIRMED);
        Appointment updated = appointmentDao.update(a);
        eventBus.publish(new AppointmentEvent(AppointmentEvent.Type.CONFIRMED, updated));
        return AppointmentView.from(updated);
    }

    public AppointmentView complete(AuthPrincipal principal, Long appointmentId, String notes) {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена: " + appointmentId));
        ensureDoctorOrAdmin(principal, a);
        if (a.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Нельзя завершить отменённую запись");
        }
        a.setStatus(AppointmentStatus.COMPLETED);
        if (notes != null && !notes.isBlank()) {
            a.setNotes(notes);
        }
        Appointment updated = appointmentDao.update(a);
        eventBus.publish(new AppointmentEvent(AppointmentEvent.Type.COMPLETED, updated));
        return AppointmentView.from(updated);
    }

    public AppointmentView markMissed(AuthPrincipal principal, Long appointmentId) {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена: " + appointmentId));
        ensureDoctorOrAdmin(principal, a);
        if (a.getStatus() != AppointmentStatus.CONFIRMED
                && a.getStatus() != AppointmentStatus.PLANNED) {
            throw new ConflictException("Нельзя пометить как пропущенную из статуса " + a.getStatus());
        }
        a.setStatus(AppointmentStatus.MISSED);
        return AppointmentView.from(appointmentDao.update(a));
    }

    public AppointmentView reschedule(AuthPrincipal principal, Long appointmentId,
                                      LocalDateTime newStartsAt, Integer newDuration) {
        Appointment a = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Запись не найдена: " + appointmentId));
        ensureCanModify(principal, a);
        if (newStartsAt == null) {
            throw new ValidationException("Новая дата/время не указаны");
        }
        if (newStartsAt.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Нельзя перенести запись на прошедшее время");
        }
        int duration = newDuration == null ? a.getDurationMinutes() : newDuration;
        if (appointmentDao.hasDoctorOverlap(a.getDoctor().getId(), newStartsAt, duration, a.getId())) {
            throw new ConflictException("Новое время уже занято");
        }
        a.setStartsAt(newStartsAt);
        a.setDurationMinutes(duration);
        a.setStatus(AppointmentStatus.PLANNED);
        Appointment updated = appointmentDao.update(a);
        reminderDao.cancelByAppointment(updated.getId());
        scheduleReminder(updated);
        eventBus.publish(new AppointmentEvent(AppointmentEvent.Type.RESCHEDULED, updated));
        return AppointmentView.from(updated);
    }

    public List<AppointmentView> myAppointments(AuthPrincipal principal) {
        return switch (principal.role()) {
            case PATIENT -> {
                Patient p = patientDao.findByUserId(principal.userId())
                        .orElseThrow(() -> new NotFoundException("Профиль пациента не найден"));
                yield appointmentDao.findByPatient(p.getId()).stream()
                        .map(AppointmentView::from).toList();
            }
            case DOCTOR -> {
                Doctor d = doctorDao.findByUserId(principal.userId())
                        .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
                LocalDateTime from = LocalDateTime.now().minusYears(1);
                LocalDateTime to = LocalDateTime.now().plusYears(1);
                yield appointmentDao.findByDoctor(d.getId(), from, to).stream()
                        .map(AppointmentView::from).toList();
            }
            case ADMIN -> appointmentDao.findAll().stream()
                    .map(AppointmentView::from).toList();
        };
    }

    public List<AppointmentView> upcomingForPatient(AuthPrincipal principal) {
        if (principal.role() != Role.PATIENT) {
            throw new ForbiddenException("Метод доступен только пациенту");
        }
        Patient p = patientDao.findByUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("Профиль пациента не найден"));
        return appointmentDao.findUpcomingForPatient(p.getId(), LocalDateTime.now())
                .stream().map(AppointmentView::from).toList();
    }

    public List<AppointmentView> doctorSchedule(AuthPrincipal principal,
                                                Long doctorId,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        long targetDoctorId;
        if (principal.role() == Role.DOCTOR) {
            Doctor d = doctorDao.findByUserId(principal.userId())
                    .orElseThrow(() -> new NotFoundException("Профиль врача не найден"));
            targetDoctorId = d.getId();
        } else if (principal.role() == Role.ADMIN) {
            if (doctorId == null) {
                throw new ValidationException("Не указан doctorId");
            }
            targetDoctorId = doctorId;
        } else {
            throw new ForbiddenException("Доступ запрещён");
        }
        if (from == null) from = LocalDateTime.now().toLocalDate().atStartOfDay();
        if (to == null) to = from.plusDays(7);
        return appointmentDao.findByDoctor(targetDoctorId, from, to).stream()
                .map(AppointmentView::from).toList();
    }

    private void scheduleReminder(Appointment a) {
        if (!AppConfig.get().getBoolean("reminder.enabled", true)) {
            return;
        }
        int leadHours = AppConfig.get().getInt("reminder.lead-hours", 24);
        LocalDateTime sendAt = a.getStartsAt().minusHours(leadHours);
        if (sendAt.isBefore(LocalDateTime.now())) {
            sendAt = LocalDateTime.now().plusMinutes(1);
        }
        Reminder reminder = new Reminder(a, sendAt, Reminder.Channel.EMAIL);
        reminderDao.save(reminder);
    }

    private void ensureCanModify(AuthPrincipal principal, Appointment a) {
        switch (principal.role()) {
            case ADMIN -> {   }
            case PATIENT -> {
                if (!a.getPatient().getUser().getId().equals(principal.userId())) {
                    throw new ForbiddenException("Можно изменить только свою запись");
                }
            }
            case DOCTOR -> {
                if (!a.getDoctor().getUser().getId().equals(principal.userId())) {
                    throw new ForbiddenException("Можно изменить только свой приём");
                }
            }
        }
    }

    private void ensureDoctorOrAdmin(AuthPrincipal principal, Appointment a) {
        if (principal.role() == Role.ADMIN) return;
        if (principal.role() == Role.DOCTOR
                && a.getDoctor().getUser().getId().equals(principal.userId())) {
            return;
        }
        throw new ForbiddenException("Только врач или администратор могут изменить статус приёма");
    }

    private static void validate(BookAppointmentRequest req) {
        if (req == null) throw new ValidationException("Тело запроса отсутствует");
        if (req.getDoctorId() == null) throw new ValidationException("doctorId обязателен");
        if (req.getStartsAt() == null) throw new ValidationException("startsAt обязателен");
    }
}

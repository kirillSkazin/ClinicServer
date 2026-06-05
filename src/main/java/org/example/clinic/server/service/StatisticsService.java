package org.example.clinic.server.service;

import org.example.clinic.server.dao.AppointmentDao;
import org.example.clinic.server.dao.DoctorDao;
import org.example.clinic.server.dao.PatientDao;
import org.example.clinic.server.dao.ReminderDao;
import org.example.clinic.server.dao.UserDao;
import org.example.clinic.server.dto.StatisticsDto;
import org.example.clinic.server.exception.ForbiddenException;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.Reminder;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.security.AuthPrincipal;

public class StatisticsService {

    private final UserDao userDao;
    private final DoctorDao doctorDao;
    private final PatientDao patientDao;
    private final AppointmentDao appointmentDao;
    private final ReminderDao reminderDao;

    public StatisticsService(UserDao userDao, DoctorDao doctorDao,
                             PatientDao patientDao, AppointmentDao appointmentDao,
                             ReminderDao reminderDao) {
        this.userDao = userDao;
        this.doctorDao = doctorDao;
        this.patientDao = patientDao;
        this.appointmentDao = appointmentDao;
        this.reminderDao = reminderDao;
    }

    public StatisticsDto getStats(AuthPrincipal principal) {
        if (principal.role() != Role.ADMIN) {
            throw new ForbiddenException("Статистика доступна только администратору");
        }
        StatisticsDto dto = new StatisticsDto();
        dto.setTotalUsers(userDao.count());
        dto.setTotalDoctors(doctorDao.count());
        dto.setTotalPatients(patientDao.count());
        dto.setTotalAppointments(appointmentDao.count());
        dto.setPlannedAppointments(appointmentDao.countByStatus(AppointmentStatus.PLANNED));
        dto.setConfirmedAppointments(appointmentDao.countByStatus(AppointmentStatus.CONFIRMED));
        dto.setCompletedAppointments(appointmentDao.countByStatus(AppointmentStatus.COMPLETED));
        dto.setCancelledAppointments(appointmentDao.countByStatus(AppointmentStatus.CANCELLED));
        dto.setMissedAppointments(appointmentDao.countByStatus(AppointmentStatus.MISSED));
        dto.setPendingReminders(reminderDao.findAll().stream()
                .filter(r -> r.getStatus() == Reminder.Status.PENDING).count());
        return dto;
    }
}

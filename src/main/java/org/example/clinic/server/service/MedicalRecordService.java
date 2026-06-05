package org.example.clinic.server.service;

import org.example.clinic.server.dao.AppointmentDao;
import org.example.clinic.server.dao.MedicalRecordDao;
import org.example.clinic.server.dao.PatientDao;
import org.example.clinic.server.dto.AddMedicalRecordRequest;
import org.example.clinic.server.dto.MedicalRecordDto;
import org.example.clinic.server.exception.ConflictException;
import org.example.clinic.server.exception.ForbiddenException;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.MedicalRecord;
import org.example.clinic.server.model.Patient;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.security.AuthPrincipal;

import java.util.List;

public class MedicalRecordService {

    private final MedicalRecordDao recordDao;
    private final AppointmentDao appointmentDao;
    private final PatientDao patientDao;

    public MedicalRecordService(MedicalRecordDao recordDao,
                                AppointmentDao appointmentDao,
                                PatientDao patientDao) {
        this.recordDao = recordDao;
        this.appointmentDao = appointmentDao;
        this.patientDao = patientDao;
    }

    public MedicalRecordDto add(AuthPrincipal principal, AddMedicalRecordRequest req) {
        if (principal.role() != Role.DOCTOR && principal.role() != Role.ADMIN) {
            throw new ForbiddenException("Только врач может добавить медицинскую запись");
        }
        if (req == null || req.getAppointmentId() == null) {
            throw new ValidationException("appointmentId обязателен");
        }
        if (req.getDiagnosis() == null || req.getDiagnosis().isBlank()) {
            throw new ValidationException("Диагноз обязателен");
        }
        Appointment appointment = appointmentDao.findById(req.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Запись на приём не найдена: " + req.getAppointmentId()));
        if (principal.role() == Role.DOCTOR
                && !appointment.getDoctor().getUser().getId().equals(principal.userId())) {
            throw new ForbiddenException("Можно добавлять записи только к своим приёмам");
        }
        if (recordDao.findByAppointmentId(appointment.getId()).isPresent()) {
            throw new ConflictException("Медицинская запись для этого приёма уже существует");
        }

        MedicalRecord record = new MedicalRecord(
                appointment, req.getDiagnosis(),
                req.getPrescription(), req.getRecommendations());
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentDao.update(appointment);
        }
        return MedicalRecordDto.from(recordDao.save(record));
    }

    public List<MedicalRecordDto> myHistory(AuthPrincipal principal) {
        if (principal.role() != Role.PATIENT) {
            throw new ForbiddenException("Метод доступен только пациенту");
        }
        Patient p = patientDao.findByUserId(principal.userId())
                .orElseThrow(() -> new NotFoundException("Профиль пациента не найден"));
        return recordDao.findByPatient(p.getId()).stream()
                .map(MedicalRecordDto::from).toList();
    }

    public List<MedicalRecordDto> historyForPatient(AuthPrincipal principal, Long patientId) {
        if (principal.role() != Role.ADMIN && principal.role() != Role.DOCTOR) {
            throw new ForbiddenException("Метод доступен только медперсоналу");
        }
        return recordDao.findByPatient(patientId).stream()
                .map(MedicalRecordDto::from).toList();
    }

    public List<MedicalRecordDto> myIssuedRecords(AuthPrincipal principal) {
        if (principal.role() != Role.DOCTOR) {
            throw new ForbiddenException("Метод доступен только врачу");
        }
        return recordDao.findByDoctorUserId(principal.userId()).stream()
                .map(MedicalRecordDto::from).toList();
    }
}

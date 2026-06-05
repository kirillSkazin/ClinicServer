package org.example.clinic.server.dao;

import org.example.clinic.server.model.MedicalRecord;

import java.util.List;
import java.util.Optional;

public class MedicalRecordDao extends GenericDao<MedicalRecord, Long> {

    public MedicalRecordDao() {
        super(MedicalRecord.class);
    }

    public Optional<MedicalRecord> findByAppointmentId(Long appointmentId) {
        return inSession(session -> session.createQuery(
                        "from MedicalRecord m where m.appointment.id = :aid",
                        MedicalRecord.class)
                .setParameter("aid", appointmentId)
                .uniqueResultOptional());
    }

    public List<MedicalRecord> findByPatient(Long patientId) {
        return inSession(session -> session.createQuery(
                        "from MedicalRecord m " +
                                "where m.appointment.patient.id = :pid " +
                                "order by m.appointment.startsAt desc",
                        MedicalRecord.class)
                .setParameter("pid", patientId)
                .getResultList());
    }

    public List<MedicalRecord> findByDoctorUserId(Long userId) {
        return inSession(session -> session.createQuery(
                        "from MedicalRecord m " +
                                "where m.appointment.doctor.user.id = :uid " +
                                "order by m.appointment.startsAt desc",
                        MedicalRecord.class)
                .setParameter("uid", userId)
                .getResultList());
    }
}

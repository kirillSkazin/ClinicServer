package org.example.clinic.server.dao;

import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public class AppointmentDao extends GenericDao<Appointment, Long> {

    public AppointmentDao() {
        super(Appointment.class);
    }

    public List<Appointment> findByDoctor(Long doctorId, LocalDateTime from, LocalDateTime to) {
        return inSession(session -> session.createQuery(
                        "from Appointment a " +
                                "where a.doctor.id = :did " +
                                "and a.startsAt between :from and :to " +
                                "order by a.startsAt", Appointment.class)
                .setParameter("did", doctorId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList());
    }

    public List<Appointment> findByPatient(Long patientId) {
        return inSession(session -> session.createQuery(
                        "from Appointment a where a.patient.id = :pid order by a.startsAt desc",
                        Appointment.class)
                .setParameter("pid", patientId)
                .getResultList());
    }

    public List<Appointment> findUpcomingForPatient(Long patientId, LocalDateTime now) {
        return inSession(session -> session.createQuery(
                        "from Appointment a " +
                                "where a.patient.id = :pid " +
                                "and a.startsAt >= :now " +
                                "and a.status in ('PLANNED','CONFIRMED') " +
                                "order by a.startsAt", Appointment.class)
                .setParameter("pid", patientId)
                .setParameter("now", now)
                .getResultList());
    }

    

    public boolean hasDoctorOverlap(Long doctorId, LocalDateTime startsAt,
                                    int durationMinutes, Long excludeId) {
        LocalDateTime endsAt = startsAt.plusMinutes(durationMinutes);
        return inSession(session -> {
            String hql = "select count(a) from Appointment a " +
                    "where a.doctor.id = :did " +
                    "and a.status in ('PLANNED','CONFIRMED') " +
                    "and a.startsAt < :ends " +
                    "and function('timestampadd', minute, a.durationMinutes, a.startsAt) > :starts " +
                    (excludeId == null ? "" : "and a.id <> :exclude ");
            
            return session.createQuery(
                            "from Appointment a where a.doctor.id = :did " +
                                    "and a.status in ('PLANNED','CONFIRMED')",
                            Appointment.class)
                    .setParameter("did", doctorId)
                    .getResultList()
                    .stream()
                    .filter(a -> excludeId == null || !excludeId.equals(a.getId()))
                    .anyMatch(a -> {
                        LocalDateTime aStart = a.getStartsAt();
                        LocalDateTime aEnd = aStart.plusMinutes(a.getDurationMinutes());
                        return aStart.isBefore(endsAt) && aEnd.isAfter(startsAt);
                    });
        });
    }

    public List<Appointment> findByStatus(AppointmentStatus status) {
        return inSession(session -> session.createQuery(
                        "from Appointment a where a.status = :s order by a.startsAt",
                        Appointment.class)
                .setParameter("s", status)
                .getResultList());
    }

    public long countByStatus(AppointmentStatus status) {
        return inSession(session -> {
            Long count = session.createQuery(
                            "select count(a) from Appointment a where a.status = :s", Long.class)
                    .setParameter("s", status)
                    .uniqueResult();
            return count == null ? 0L : count;
        });
    }
}

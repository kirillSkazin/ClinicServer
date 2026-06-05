package org.example.clinic.server.dao;

import org.example.clinic.server.model.Reminder;

import java.time.LocalDateTime;
import java.util.List;

public class ReminderDao extends GenericDao<Reminder, Long> {

    public ReminderDao() {
        super(Reminder.class);
    }

    public List<Reminder> findDueForSending(LocalDateTime now, int limit) {
        return inSession(session -> session.createQuery(
                        "from Reminder r where r.status = 'PENDING' " +
                                "and r.sendAt <= :now order by r.sendAt", Reminder.class)
                .setParameter("now", now)
                .setMaxResults(limit)
                .getResultList());
    }

    public List<Reminder> findByAppointment(Long appointmentId) {
        return inSession(session -> session.createQuery(
                        "from Reminder r where r.appointment.id = :aid order by r.sendAt",
                        Reminder.class)
                .setParameter("aid", appointmentId)
                .getResultList());
    }

    public int cancelByAppointment(Long appointmentId) {
        return inTransaction(session -> session.createMutationQuery(
                        "update Reminder r set r.status = 'CANCELLED' " +
                                "where r.appointment.id = :aid and r.status = 'PENDING'")
                .setParameter("aid", appointmentId)
                .executeUpdate());
    }
}

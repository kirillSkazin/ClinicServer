package org.example.clinic.server.dao;

import org.example.clinic.server.model.DoctorSchedule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public class DoctorScheduleDao extends GenericDao<DoctorSchedule, Long> {

    public DoctorScheduleDao() {
        super(DoctorSchedule.class);
    }

    public List<DoctorSchedule> findByDoctor(Long doctorId) {
        return inSession(session -> session.createQuery(
                        "from DoctorSchedule s where s.doctor.id = :did order by s.dayOfWeek",
                        DoctorSchedule.class)
                .setParameter("did", doctorId)
                .getResultList());
    }

    public Optional<DoctorSchedule> findByDoctorAndDay(Long doctorId, DayOfWeek day) {
        return inSession(session -> session.createQuery(
                        "from DoctorSchedule s where s.doctor.id = :did and s.dayOfWeek = :d",
                        DoctorSchedule.class)
                .setParameter("did", doctorId)
                .setParameter("d", day)
                .uniqueResultOptional());
    }
}

package org.example.clinic.server.dao;

import org.example.clinic.server.model.Doctor;

import java.util.List;
import java.util.Optional;

public class DoctorDao extends GenericDao<Doctor, Long> {

    public DoctorDao() {
        super(Doctor.class);
    }

    public Optional<Doctor> findByUserId(Long userId) {
        return inSession(session -> session.createQuery(
                        "from Doctor d where d.user.id = :uid", Doctor.class)
                .setParameter("uid", userId)
                .uniqueResultOptional());
    }

    public List<Doctor> findBySpecialization(Long specializationId) {
        return inSession(session -> session.createQuery(
                        "from Doctor d where d.specialization.id = :sid order by d.user.fullName",
                        Doctor.class)
                .setParameter("sid", specializationId)
                .getResultList());
    }

    public List<Doctor> searchByName(String fragment) {
        return inSession(session -> session.createQuery(
                        "from Doctor d where lower(d.user.fullName) like :q order by d.user.fullName",
                        Doctor.class)
                .setParameter("q", "%" + fragment.toLowerCase() + "%")
                .getResultList());
    }
}

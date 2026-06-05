package org.example.clinic.server.dao;

import org.example.clinic.server.model.Patient;

import java.util.Optional;

public class PatientDao extends GenericDao<Patient, Long> {

    public PatientDao() {
        super(Patient.class);
    }

    public Optional<Patient> findByUserId(Long userId) {
        return inSession(session -> session.createQuery(
                        "from Patient p where p.user.id = :uid", Patient.class)
                .setParameter("uid", userId)
                .uniqueResultOptional());
    }
}

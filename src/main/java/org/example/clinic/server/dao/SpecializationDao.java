package org.example.clinic.server.dao;

import org.example.clinic.server.model.Specialization;

import java.util.Optional;

public class SpecializationDao extends GenericDao<Specialization, Long> {

    public SpecializationDao() {
        super(Specialization.class);
    }

    public Optional<Specialization> findByName(String name) {
        return inSession(session -> session.createQuery(
                        "from Specialization s where lower(s.name) = lower(:n)",
                        Specialization.class)
                .setParameter("n", name)
                .uniqueResultOptional());
    }
}

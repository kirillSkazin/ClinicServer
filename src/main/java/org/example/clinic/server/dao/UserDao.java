package org.example.clinic.server.dao;

import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.User;

import java.util.List;
import java.util.Optional;

public class UserDao extends GenericDao<User, Long> {

    public UserDao() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return inSession(session -> session.createQuery(
                        "from User u where u.username = :u", User.class)
                .setParameter("u", username)
                .uniqueResultOptional());
    }

    public Optional<User> findByEmail(String email) {
        return inSession(session -> session.createQuery(
                        "from User u where u.email = :e", User.class)
                .setParameter("e", email)
                .uniqueResultOptional());
    }

    public boolean existsByUsername(String username) {
        return inSession(session -> {
            Long count = session.createQuery(
                            "select count(u) from User u where u.username = :u", Long.class)
                    .setParameter("u", username)
                    .uniqueResult();
            return count != null && count > 0;
        });
    }

    public boolean existsByEmail(String email) {
        return inSession(session -> {
            Long count = session.createQuery(
                            "select count(u) from User u where u.email = :e", Long.class)
                    .setParameter("e", email)
                    .uniqueResult();
            return count != null && count > 0;
        });
    }

    public List<User> findByRole(Role role) {
        return inSession(session -> session.createQuery(
                        "from User u where u.role = :r order by u.fullName", User.class)
                .setParameter("r", role)
                .getResultList());
    }
}

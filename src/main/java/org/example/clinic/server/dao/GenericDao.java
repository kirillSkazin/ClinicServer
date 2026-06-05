package org.example.clinic.server.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.example.clinic.server.config.HibernateUtil;
import org.example.clinic.server.exception.ServiceException;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;


public abstract class GenericDao<T, ID extends Serializable> implements Repository<T, ID> {

    protected final Class<T> entityClass;

    protected GenericDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected SessionFactory sessionFactory() {
        return HibernateUtil.getSessionFactory();
    }

    

    public <R> R inTransaction(Function<Session, R> work) {
        try (Session session = sessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                R result = work.apply(session);
                tx.commit();
                return result;
            } catch (RuntimeException ex) {
                if (tx.isActive()) {
                    try { tx.rollback(); } catch (HibernateException ignored) {   }
                }
                throw ex;
            }
        }
    }

    
    public <R> R inSession(Function<Session, R> work) {
        try (Session session = sessionFactory().openSession()) {
            return work.apply(session);
        }
    }

    @Override
    public T save(T entity) {
        return inTransaction(session -> {
            session.persist(entity);
            return entity;
        });
    }

    @Override
    public T update(T entity) {
        return inTransaction(session -> session.merge(entity));
    }

    @Override
    public Optional<T> findById(ID id) {
        return inSession(session -> Optional.ofNullable(session.get(entityClass, id)));
    }

    @Override
    public List<T> findAll() {
        return inSession(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(entityClass);
            Root<T> root = cq.from(entityClass);
            cq.select(root);
            return session.createQuery(cq).getResultList();
        });
    }

    @Override
    public void delete(T entity) {
        inTransaction(session -> {
            session.remove(session.contains(entity) ? entity : session.merge(entity));
            return null;
        });
    }

    @Override
    public boolean deleteById(ID id) {
        return Boolean.TRUE.equals(inTransaction(session -> {
            T entity = session.get(entityClass, id);
            if (entity == null) {
                return false;
            }
            session.remove(entity);
            return true;
        }));
    }

    @Override
    public long count() {
        return inSession(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            cq.select(cb.count(cq.from(entityClass)));
            Long result = session.createQuery(cq).getSingleResult();
            return result == null ? 0L : result;
        });
    }

    
    protected static ServiceException wrap(String code, String message, Throwable cause) {
        return new ServiceException(code, message, cause);
    }
}

package org.example.clinic.server.config;

import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.Doctor;
import org.example.clinic.server.model.DoctorSchedule;
import org.example.clinic.server.model.MedicalRecord;
import org.example.clinic.server.model.Patient;
import org.example.clinic.server.model.Reminder;
import org.example.clinic.server.model.Specialization;
import org.example.clinic.server.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


public final class HibernateUtil {

    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);

    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {
    }

    public static SessionFactory getSessionFactory() {
        SessionFactory local = sessionFactory;
        if (local == null) {
            synchronized (HibernateUtil.class) {
                local = sessionFactory;
                if (local == null) {
                    local = build();
                    sessionFactory = local;
                }
            }
        }
        return local;
    }

    private static SessionFactory build() {
        AppConfig cfg = AppConfig.get();
        Properties props = new Properties();

        props.put(AvailableSettings.JAKARTA_JDBC_DRIVER, cfg.getString("db.driver"));
        props.put(AvailableSettings.JAKARTA_JDBC_URL, cfg.getString("db.url"));
        props.put(AvailableSettings.JAKARTA_JDBC_USER, cfg.getString("db.username"));
        props.put(AvailableSettings.JAKARTA_JDBC_PASSWORD, cfg.getString("db.password"));
        props.put(AvailableSettings.DIALECT, cfg.getString("db.dialect"));
        props.put(AvailableSettings.HBM2DDL_AUTO, cfg.getString("db.hbm2ddl"));
        props.put(AvailableSettings.SHOW_SQL, String.valueOf(cfg.getBoolean("db.show-sql", false)));
        props.put(AvailableSettings.FORMAT_SQL, String.valueOf(cfg.getBoolean("db.format-sql", false)));
        props.put(AvailableSettings.USE_SQL_COMMENTS, "false");

        
        props.put(AvailableSettings.CONNECTION_PROVIDER,
                "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        props.put("hibernate.hikari.maximumPoolSize",
                String.valueOf(cfg.getInt("db.pool-size", 10)));
        props.put("hibernate.hikari.idleTimeout", "30000");
        props.put("hibernate.hikari.connectionTimeout", "10000");

        Configuration configuration = new Configuration().setProperties(props);
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Specialization.class);
        configuration.addAnnotatedClass(Doctor.class);
        configuration.addAnnotatedClass(Patient.class);
        configuration.addAnnotatedClass(Appointment.class);
        configuration.addAnnotatedClass(MedicalRecord.class);
        configuration.addAnnotatedClass(Reminder.class);
        configuration.addAnnotatedClass(DoctorSchedule.class);

        SessionFactory sf = configuration.buildSessionFactory();
        log.info("Hibernate SessionFactory initialized for url={}", cfg.getString("db.url"));
        return sf;
    }

    public static void shutdown() {
        SessionFactory local = sessionFactory;
        if (local != null) {
            local.close();
            sessionFactory = null;
            log.info("Hibernate SessionFactory closed");
        }
    }
}

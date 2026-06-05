package org.example.clinic.server;

import org.example.clinic.server.config.AppConfig;
import org.example.clinic.server.config.HibernateUtil;
import org.example.clinic.server.controller.Controllers;
import org.example.clinic.server.dao.DaoFactory;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.User;
import org.example.clinic.server.network.RequestDispatcher;
import org.example.clinic.server.network.TcpServer;
import org.example.clinic.server.notification.AppointmentEvent;
import org.example.clinic.server.notification.AppointmentEventBus;
import org.example.clinic.server.notification.ConsoleNotificationStrategy;
import org.example.clinic.server.notification.EmailNotificationStrategy;
import org.example.clinic.server.notification.NotificationStrategy;
import org.example.clinic.server.security.JwtService;
import org.example.clinic.server.security.PasswordEncoder;
import org.example.clinic.server.service.AppointmentService;
import org.example.clinic.server.service.AuthService;
import org.example.clinic.server.service.DoctorService;
import org.example.clinic.server.service.MedicalRecordService;
import org.example.clinic.server.service.PatientService;
import org.example.clinic.server.service.ReminderService;
import org.example.clinic.server.service.ScheduleService;
import org.example.clinic.server.service.SpecializationService;
import org.example.clinic.server.service.StatisticsService;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public final class ServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

    private ServerApplication() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.initialize(args);

        try {
            HibernateUtil.getSessionFactory();
        } catch (RuntimeException ex) {
            log.error("Can't initialize Hibernate. Check connection to PostgreSQL " +
                    "and that 'hibernate-hikaricp' is on the classpath.", ex);
            System.exit(1);
        }

        DaoFactory dao = DaoFactory.get();
        PasswordEncoder passwordEncoder = new PasswordEncoder(config.getInt("security.bcrypt.cost", 10));
        JwtService jwtService = new JwtService(
                config.getString("security.jwt.secret"),
                config.getString("security.jwt.issuer"),
                config.getInt("security.jwt.access-ttl-minutes"));

        bootstrapAdminIfNeeded(config, dao, passwordEncoder);
        bootstrapSeedDataIfNeeded(config, passwordEncoder);

        AppointmentEventBus eventBus = new AppointmentEventBus();
        eventBus.subscribe(event -> log.info("[event] {} for appointment #{}",
                event.type(), event.appointment() != null ? event.appointment().getId() : null));

        AuthService authService = new AuthService(dao.userDao(), dao.patientDao(),
                passwordEncoder, jwtService);
        SpecializationService specializationService = new SpecializationService(dao.specializationDao());
        DoctorService doctorService = new DoctorService(dao.doctorDao(), dao.userDao(),
                dao.specializationDao(), passwordEncoder);
        PatientService patientService = new PatientService(dao.patientDao());
        ScheduleService scheduleService = new ScheduleService(dao.doctorScheduleDao(),
                dao.doctorDao(), dao.appointmentDao());
        AppointmentService appointmentService = new AppointmentService(
                dao.appointmentDao(), dao.doctorDao(), dao.patientDao(),
                dao.reminderDao(), eventBus);
        MedicalRecordService recordService = new MedicalRecordService(
                dao.medicalRecordDao(), dao.appointmentDao(), dao.patientDao());
        StatisticsService statisticsService = new StatisticsService(
                dao.userDao(), dao.doctorDao(), dao.patientDao(),
                dao.appointmentDao(), dao.reminderDao());

        NotificationStrategy notificationStrategy = createNotificationStrategy(config);

        ReminderService reminderService = null;
        if (config.getBoolean("reminder.enabled", true)) {
            reminderService = new ReminderService(
                    dao.reminderDao(),
                    notificationStrategy,
                    config.getLong("reminder.scan-period-seconds", 60),
                    config.getInt("reminder.worker-threads", 2));
            reminderService.start();
            
            ReminderService finalRemindSvc = reminderService;
            eventBus.subscribe(e -> {
                if (e.type() == AppointmentEvent.Type.CANCELLED && e.appointment() != null) {
                    dao.reminderDao().cancelByAppointment(e.appointment().getId());
                }
            });
        }

        RequestDispatcher dispatcher = new RequestDispatcher(jwtService);
        Controllers.registerAll(dispatcher, authService, doctorService, patientService,
                specializationService, appointmentService, recordService,
                scheduleService, statisticsService);
        log.info("Registered {} commands", dispatcher.routes().size());

        TcpServer server = new TcpServer(
                config.getString("server.host"),
                config.getInt("server.port"),
                config.getInt("server.backlog", 100),
                config.getInt("server.thread-pool-size", 32),
                config.getInt("server.socket-timeout-ms", 60000),
                dispatcher);

        ReminderService finalReminderService = reminderService;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            try { server.close(); } catch (Exception ignored) {   }
            if (finalReminderService != null) {
                try { finalReminderService.close(); } catch (Exception ignored) {   }
            }
            HibernateUtil.shutdown();
        }, "shutdown-hook"));

        try {
            server.start();
        } catch (Exception ex) {
            log.error("Не удалось стартовать TCP-сервер: {}", ex.getMessage(), ex);
            System.exit(2);
        }

        log.info("Clinic server started successfully");
    }

    private static NotificationStrategy createNotificationStrategy(AppConfig config) {
        if (config.getBoolean("mail.enabled", false)) {
            log.info("Notifications: SMTP via {}:{}",
                    config.getString("mail.smtp.host"),
                    config.getInt("mail.smtp.port"));
            return new EmailNotificationStrategy(
                    config.getString("mail.smtp.host"),
                    config.getInt("mail.smtp.port"),
                    config.getBoolean("mail.smtp.starttls", true),
                    config.getBoolean("mail.smtp.auth", true),
                    config.getString("mail.username"),
                    config.getString("mail.password"),
                    config.getString("mail.from"),
                    config.getString("mail.from-name", null));
        }
        log.info("Notifications: console-only (mail.enabled=false)");
        return new ConsoleNotificationStrategy();
    }

    private static void bootstrapAdminIfNeeded(AppConfig config, DaoFactory dao,
                                               PasswordEncoder passwordEncoder) {
        if (!config.getBoolean("bootstrap.create-default-admin", true)) {
            return;
        }
        if (dao.userDao().count() > 0) {
            return;
        }
        String username = config.getString("bootstrap.admin.username");
        String password = config.getString("bootstrap.admin.password");
        String email = config.getString("bootstrap.admin.email");
        String fullName = config.getString("bootstrap.admin.full-name");
        User admin = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.hash(password))
                .email(email)
                .fullName(fullName)
                .role(Role.ADMIN)
                .active(true)
                .build();
        dao.userDao().save(admin);
        log.warn("Создан стартовый администратор: '{}'. Смените пароль после первого входа.", username);
    }

    private static void bootstrapSeedDataIfNeeded(AppConfig config, PasswordEncoder passwordEncoder) {
        if (!config.getBoolean("bootstrap.seed-default-data", true)) {
            return;
        }
        String resource = config.getString("bootstrap.seed-resource", "db/seed.sql");
        String seedPassword = config.getString("bootstrap.seed-user-password", "password");
        String script = readResource(resource)
                .replace("__SEEDED_USER_PASSWORD_HASH__", passwordEncoder.hash(seedPassword));
        List<String> statements = splitSqlStatements(script);
        if (statements.isEmpty()) {
            log.info("Seed script '{}' is empty", resource);
            return;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                session.doWork(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        for (String sql : statements) {
                            statement.execute(sql);
                        }
                    }
                });
                tx.commit();
                log.info("Seed script '{}' applied successfully ({} statements)", resource, statements.size());
            } catch (RuntimeException ex) {
                if (tx.isActive()) {
                    tx.rollback();
                }
                throw ex;
            }
        }
    }

    private static String readResource(String resource) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Seed resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read seed resource: " + resource, ex);
        }
    }

    private static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (c == '\'') {
                current.append(c);
                if (inString && i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                    current.append(script.charAt(++i));
                } else {
                    inString = !inString;
                }
                continue;
            }
            if (c == ';' && !inString) {
                String sql = current.toString().trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String sql = current.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
        return statements;
    }
}

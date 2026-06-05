package org.example.clinic.server.dao;


public class DaoFactory {

    private static volatile DaoFactory instance = new DaoFactory();

    private final UserDao userDao = new UserDao();
    private final SpecializationDao specializationDao = new SpecializationDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final PatientDao patientDao = new PatientDao();
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final MedicalRecordDao medicalRecordDao = new MedicalRecordDao();
    private final ReminderDao reminderDao = new ReminderDao();
    private final DoctorScheduleDao doctorScheduleDao = new DoctorScheduleDao();

    public static DaoFactory get() {
        return instance;
    }

    
    public static void install(DaoFactory replacement) {
        instance = replacement;
    }

    public UserDao userDao() { return userDao; }
    public SpecializationDao specializationDao() { return specializationDao; }
    public DoctorDao doctorDao() { return doctorDao; }
    public PatientDao patientDao() { return patientDao; }
    public AppointmentDao appointmentDao() { return appointmentDao; }
    public MedicalRecordDao medicalRecordDao() { return medicalRecordDao; }
    public ReminderDao reminderDao() { return reminderDao; }
    public DoctorScheduleDao doctorScheduleDao() { return doctorScheduleDao; }
}

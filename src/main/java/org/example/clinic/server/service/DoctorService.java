package org.example.clinic.server.service;

import org.example.clinic.server.dao.DoctorDao;
import org.example.clinic.server.dao.SpecializationDao;
import org.example.clinic.server.dao.UserDao;
import org.example.clinic.server.dto.CreateDoctorRequest;
import org.example.clinic.server.dto.DoctorView;
import org.example.clinic.server.exception.ConflictException;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Doctor;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.Specialization;
import org.example.clinic.server.model.User;
import org.example.clinic.server.security.PasswordEncoder;

import java.util.List;

public class DoctorService {

    private final DoctorDao doctorDao;
    private final UserDao userDao;
    private final SpecializationDao specializationDao;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorDao doctorDao,
                         UserDao userDao,
                         SpecializationDao specializationDao,
                         PasswordEncoder passwordEncoder) {
        this.doctorDao = doctorDao;
        this.userDao = userDao;
        this.specializationDao = specializationDao;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DoctorView> list() {
        return doctorDao.findAll().stream().map(DoctorView::from).toList();
    }

    public List<DoctorView> findBySpecialization(Long specializationId) {
        return doctorDao.findBySpecialization(specializationId)
                .stream().map(DoctorView::from).toList();
    }

    public List<DoctorView> searchByName(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return list();
        }
        return doctorDao.searchByName(fragment).stream().map(DoctorView::from).toList();
    }

    public DoctorView getById(Long id) {
        Doctor d = doctorDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Врач не найден: " + id));
        return DoctorView.from(d);
    }

    
    public Doctor requireByUserId(Long userId) {
        return doctorDao.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль врача не найден для пользователя: " + userId));
    }

    public DoctorView create(CreateDoctorRequest req) {
        validate(req);
        if (userDao.existsByUsername(req.getUsername())) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        if (userDao.existsByEmail(req.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        Specialization spec = specializationDao.findById(req.getSpecializationId())
                .orElseThrow(() -> new NotFoundException("Специализация не найдена: " + req.getSpecializationId()));

        User user = User.builder()
                .username(req.getUsername().trim())
                .passwordHash(passwordEncoder.hash(req.getPassword()))
                .email(req.getEmail().trim().toLowerCase())
                .fullName(req.getFullName().trim())
                .phone(req.getPhone())
                .role(Role.DOCTOR)
                .active(true)
                .build();
        User savedUser = userDao.save(user);

        Doctor doctor = new Doctor(savedUser, spec, req.getRoomNumber(),
                req.getExperienceYears(), req.getBio());
        return DoctorView.from(doctorDao.save(doctor));
    }

    public DoctorView update(Long id, CreateDoctorRequest req) {
        Doctor doctor = doctorDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Врач не найден: " + id));

        if (req.getSpecializationId() != null) {
            Specialization spec = specializationDao.findById(req.getSpecializationId())
                    .orElseThrow(() -> new NotFoundException("Специализация не найдена: " + req.getSpecializationId()));
            doctor.setSpecialization(spec);
        }
        if (req.getRoomNumber() != null) doctor.setRoomNumber(req.getRoomNumber());
        if (req.getExperienceYears() != null) doctor.setExperienceYears(req.getExperienceYears());
        if (req.getBio() != null) doctor.setBio(req.getBio());

        User u = doctor.getUser();
        if (req.getEmail() != null && !req.getEmail().isBlank()) u.setEmail(req.getEmail().trim().toLowerCase());
        if (req.getFullName() != null && !req.getFullName().isBlank()) u.setFullName(req.getFullName().trim());
        if (req.getPhone() != null) u.setPhone(req.getPhone());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPasswordHash(passwordEncoder.hash(req.getPassword()));
        }
        userDao.update(u);

        return DoctorView.from(doctorDao.update(doctor));
    }

    public void delete(Long id) {
        Doctor doctor = doctorDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Врач не найден: " + id));
        User u = doctor.getUser();
        u.setActive(false);
        userDao.update(u);
    }

    private static void validate(CreateDoctorRequest req) {
        if (req == null) throw new ValidationException("Тело запроса отсутствует");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new ValidationException("Логин обязателен");
        if (req.getPassword() == null || req.getPassword().length() < 6)
            throw new ValidationException("Пароль должен быть не короче 6 символов");
        if (req.getEmail() == null || !req.getEmail().contains("@"))
            throw new ValidationException("Некорректный email");
        if (req.getFullName() == null || req.getFullName().isBlank())
            throw new ValidationException("ФИО врача обязательно");
        if (req.getSpecializationId() == null)
            throw new ValidationException("Специализация обязательна");
    }
}

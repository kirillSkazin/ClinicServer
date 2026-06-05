package org.example.clinic.server.service;

import org.example.clinic.server.dao.PatientDao;
import org.example.clinic.server.dao.UserDao;
import org.example.clinic.server.dto.LoginRequest;
import org.example.clinic.server.dto.LoginResponse;
import org.example.clinic.server.dto.RegisterPatientRequest;
import org.example.clinic.server.exception.AuthException;
import org.example.clinic.server.exception.ConflictException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Patient;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.model.User;
import org.example.clinic.server.security.JwtService;
import org.example.clinic.server.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final PatientDao patientDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserDao userDao,
                       PatientDao patientDao,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userDao = userDao;
        this.patientDao = patientDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        validate(request);
        User user = userDao.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException("Неверный логин или пароль"));
        if (!user.isActive()) {
            throw new AuthException("Учётная запись отключена");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("Неверный логин или пароль");
        }
        String token = jwtService.issueToken(user.getId(), user.getUsername(), user.getRole());
        log.info("User '{}' logged in (role={})", user.getUsername(), user.getRole());
        return new LoginResponse(
                token,
                jwtService.getAccessTtl().toSeconds(),
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole());
    }

    public LoginResponse registerPatient(RegisterPatientRequest req) {
        validate(req);
        if (userDao.existsByUsername(req.getUsername())) {
            throw new ConflictException("Пользователь с таким логином уже существует");
        }
        if (userDao.existsByEmail(req.getEmail())) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }
        User user = User.builder()
                .username(req.getUsername().trim())
                .passwordHash(passwordEncoder.hash(req.getPassword()))
                .email(req.getEmail().trim().toLowerCase())
                .fullName(req.getFullName().trim())
                .phone(req.getPhone())
                .role(Role.PATIENT)
                .active(true)
                .build();
        User saved = userDao.save(user);

        Patient patient = new Patient(saved, req.getBirthDate(), req.getAddress(), req.getInsuranceNumber());
        patientDao.save(patient);

        log.info("Patient '{}' registered", saved.getUsername());

        String token = jwtService.issueToken(saved.getId(), saved.getUsername(), saved.getRole());
        return new LoginResponse(
                token,
                jwtService.getAccessTtl().toSeconds(),
                saved.getId(),
                saved.getUsername(),
                saved.getFullName(),
                saved.getRole());
    }

    private void validate(LoginRequest req) {
        if (req == null) {
            throw new ValidationException("Тело запроса отсутствует");
        }
        if (isBlank(req.getUsername())) {
            throw new ValidationException("Поле 'username' обязательно");
        }
        if (isBlank(req.getPassword())) {
            throw new ValidationException("Поле 'password' обязательно");
        }
    }

    private void validate(RegisterPatientRequest req) {
        if (req == null) {
            throw new ValidationException("Тело запроса отсутствует");
        }
        if (isBlank(req.getUsername()) || req.getUsername().length() < 3) {
            throw new ValidationException("Логин должен быть не короче 3 символов");
        }
        if (isBlank(req.getPassword()) || req.getPassword().length() < 6) {
            throw new ValidationException("Пароль должен быть не короче 6 символов");
        }
        if (isBlank(req.getEmail()) || !req.getEmail().contains("@")) {
            throw new ValidationException("Некорректный email");
        }
        if (isBlank(req.getFullName())) {
            throw new ValidationException("Поле 'fullName' обязательно");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

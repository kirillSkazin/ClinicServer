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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret";

    @Mock UserDao userDao;
    @Mock PatientDao patientDao;
    @Mock PasswordEncoder passwordEncoder;
    private JwtService jwtService;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() {
        jwtService = new JwtService(SECRET, "test-iss", 60);
        authService = new AuthService(userDao, patientDao, passwordEncoder, jwtService);
    }

    @Test
    void loginReturnsTokenWhenCredentialsMatch() {
        User user = User.builder()
                .username("alice")
                .passwordHash("hashed")
                .email("a@a.com")
                .fullName("Alice A")
                .role(Role.PATIENT)
                .active(true)
                .build();
        user.setId(1L);
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("p", "hashed")).thenReturn(true);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("p");

        LoginResponse resp = authService.login(req);
        assertNotNull(resp.getToken());
        assertEquals("alice", resp.getUsername());
        assertEquals(Role.PATIENT, resp.getRole());
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        when(userDao.findByUsername("nope")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest();
        req.setUsername("nope");
        req.setPassword("p");

        assertThrows(AuthException.class, () -> authService.login(req));
    }

    @Test
    void loginThrowsWhenPasswordWrong() {
        User user = User.builder()
                .username("alice").passwordHash("hashed").email("a@a.com")
                .fullName("Alice").role(Role.PATIENT).active(true).build();
        user.setId(1L);
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("bad");

        assertThrows(AuthException.class, () -> authService.login(req));
    }

    @Test
    void loginThrowsWhenAccountInactive() {
        User user = User.builder()
                .username("alice").passwordHash("hashed").email("a@a.com")
                .fullName("Alice").role(Role.PATIENT).active(false).build();
        user.setId(1L);
        when(userDao.findByUsername("alice")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("p");

        assertThrows(AuthException.class, () -> authService.login(req));
    }

    @Test
    void registerPatientSavesUserAndPatient() {
        when(userDao.existsByUsername("bob")).thenReturn(false);
        when(userDao.existsByEmail("b@b.com")).thenReturn(false);
        when(passwordEncoder.hash("password1")).thenReturn("HASH");
        when(userDao.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(7L);
            return u;
        });
        when(patientDao.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setUsername("bob");
        req.setPassword("password1");
        req.setEmail("b@b.com");
        req.setFullName("Bob B");

        LoginResponse resp = authService.registerPatient(req);

        assertEquals("bob", resp.getUsername());
        assertEquals(Role.PATIENT, resp.getRole());
        verify(userDao, times(1)).save(any(User.class));
        verify(patientDao, times(1)).save(any(Patient.class));
    }

    @Test
    void registerPatientFailsOnDuplicateUsername() {
        when(userDao.existsByUsername("bob")).thenReturn(true);
        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setUsername("bob");
        req.setPassword("password1");
        req.setEmail("b@b.com");
        req.setFullName("Bob");

        assertThrows(ConflictException.class, () -> authService.registerPatient(req));
        verify(userDao, never()).save(any(User.class));
    }

    @Test
    void registerPatientValidatesInput() {
        RegisterPatientRequest req = new RegisterPatientRequest();
        req.setUsername("ab");
        req.setPassword("12345");
        req.setEmail("not-email");
        assertThrows(ValidationException.class, () -> authService.registerPatient(req));
    }
}

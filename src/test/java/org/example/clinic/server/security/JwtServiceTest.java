package org.example.clinic.server.security;

import org.example.clinic.server.exception.AuthException;
import org.example.clinic.server.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET =
            "test-secret-test-secret-test-secret-test-secret-test-secret";

    @Test
    void issuedTokenIsParsedBackToOriginalPrincipal() {
        JwtService svc = new JwtService(SECRET, "test-issuer", 60);
        String token = svc.issueToken(42L, "alice", Role.PATIENT);
        AuthPrincipal principal = svc.parse(token);

        assertEquals(42L, principal.userId());
        assertEquals("alice", principal.username());
        assertEquals(Role.PATIENT, principal.role());
    }

    @Test
    void parseFailsForInvalidToken() {
        JwtService svc = new JwtService(SECRET, "test-issuer", 60);
        assertThrows(AuthException.class, () -> svc.parse("not-a-jwt"));
    }

    @Test
    void parseFailsForBlankToken() {
        JwtService svc = new JwtService(SECRET, "test-issuer", 60);
        assertThrows(AuthException.class, () -> svc.parse(""));
    }

    @Test
    void shortSecretIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("too-short", "iss", 5));
    }

    @Test
    void issuerMismatchIsRejected() {
        JwtService a = new JwtService(SECRET, "iss-a", 60);
        JwtService b = new JwtService(SECRET, "iss-b", 60);
        String token = a.issueToken(1L, "u", Role.ADMIN);
        assertNotNull(token);
        assertThrows(AuthException.class, () -> b.parse(token));
    }
}

package org.example.clinic.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder(4);

    @Test
    void hashIsNotEqualToRawPassword() {
        String raw = "secret123";
        String hash = encoder.hash(raw);
        assertNotNull(hash);
        assertNotEquals(raw, hash);
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = encoder.hash("p@ssw0rd");
        assertTrue(encoder.matches("p@ssw0rd", hash));
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = encoder.hash("p@ssw0rd");
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void matchesIsNullSafe() {
        assertFalse(encoder.matches(null, null));
        assertFalse(encoder.matches("any", null));
        assertFalse(encoder.matches(null, "$2a$04$invalid"));
    }

    @Test
    void costMustBeInValidRange() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordEncoder(3));
        assertThrows(IllegalArgumentException.class, () -> new PasswordEncoder(32));
    }
}

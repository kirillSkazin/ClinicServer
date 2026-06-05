package org.example.clinic.server.security;

import org.mindrot.jbcrypt.BCrypt;


public final class PasswordEncoder {

    private final int cost;

    public PasswordEncoder(int cost) {
        if (cost < 4 || cost > 31) {
            throw new IllegalArgumentException("BCrypt cost must be in [4, 31]");
        }
        this.cost = cost;
    }

    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(cost));
    }

    public boolean matches(String rawPassword, String hashed) {
        if (rawPassword == null || hashed == null || hashed.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashed);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public int getCost() {
        return cost;
    }
}

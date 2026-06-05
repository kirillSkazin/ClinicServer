package org.example.clinic.server.security;

import org.example.clinic.server.model.Role;


public record AuthPrincipal(Long userId, String username, Role role) {

    public boolean hasRole(Role required) {
        return role == required;
    }

    public boolean hasAnyRole(Role... roles) {
        if (roles == null) {
            return false;
        }
        for (Role r : roles) {
            if (role == r) {
                return true;
            }
        }
        return false;
    }
}

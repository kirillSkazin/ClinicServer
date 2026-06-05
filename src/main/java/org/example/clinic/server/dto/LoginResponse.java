package org.example.clinic.server.dto;

import org.example.clinic.server.model.Role;

public class LoginResponse {
    private String token;
    private long expiresInSeconds;
    private Long userId;
    private String username;
    private String fullName;
    private Role role;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiresInSeconds, Long userId,
                         String username, String fullName, Role role) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}

package org.example.clinic.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.example.clinic.server.exception.AuthException;
import org.example.clinic.server.model.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;


public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_USERNAME = "sub";
    public static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(String secret, String issuer, long accessTtlMinutes) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes long for HMAC-SHA256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
    }

    public String issueToken(long userId, String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public AuthPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Token is empty");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = claims.get(CLAIM_USER_ID, Number.class).longValue();
            String username = claims.getSubject();
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class));
            return new AuthPrincipal(userId, username, role);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AuthException("Invalid or expired token: " + ex.getMessage());
        }
    }

    public Duration getAccessTtl() {
        return accessTtl;
    }
}

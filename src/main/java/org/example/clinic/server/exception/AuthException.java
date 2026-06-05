package org.example.clinic.server.exception;


public class AuthException extends ServiceException {
    public AuthException(String message) {
        super("UNAUTHORIZED", message);
    }
}

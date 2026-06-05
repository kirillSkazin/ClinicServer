package org.example.clinic.server.exception;


public class ForbiddenException extends ServiceException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}

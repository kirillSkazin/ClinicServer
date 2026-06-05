package org.example.clinic.server.exception;


public class ConflictException extends ServiceException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}

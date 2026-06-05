package org.example.clinic.server.exception;


public class ValidationException extends ServiceException {
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}

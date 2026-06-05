package org.example.clinic.server.exception;


public class NotFoundException extends ServiceException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}

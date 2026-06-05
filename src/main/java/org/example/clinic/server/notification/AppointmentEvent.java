package org.example.clinic.server.notification;

import org.example.clinic.server.model.Appointment;


public record AppointmentEvent(Type type, Appointment appointment) {

    public enum Type {
        BOOKED,
        CONFIRMED,
        RESCHEDULED,
        CANCELLED,
        COMPLETED
    }
}

package org.example.clinic.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;


@Entity
@Table(name = "appointments",
        indexes = {
                @Index(name = "ix_appointments_doctor_time", columnList = "doctor_id,starts_at"),
                @Index(name = "ix_appointments_patient_time", columnList = "patient_id,starts_at"),
                @Index(name = "ix_appointments_status", columnList = "status")
        })
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointments_doctor"))
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_appointments_patient"))
    private Patient patient;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AppointmentStatus status = AppointmentStatus.PLANNED;

    @Column(name = "complaint", length = 1024)
    private String complaint;

    @Column(name = "notes", length = 2048)
    private String notes;

    public Appointment() {
    }

    public LocalDateTime getEndsAt() {
        return startsAt == null ? null : startsAt.plusMinutes(durationMinutes);
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getComplaint() {
        return complaint;
    }

    public void setComplaint(String complaint) {
        this.complaint = complaint;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static Builder builder() {
        return new Builder();
    }

    
    public static final class Builder {
        private Doctor doctor;
        private Patient patient;
        private LocalDateTime startsAt;
        private int durationMinutes = 30;
        private AppointmentStatus status = AppointmentStatus.PLANNED;
        private String complaint;
        private String notes;

        public Builder doctor(Doctor v) { this.doctor = v; return this; }
        public Builder patient(Patient v) { this.patient = v; return this; }
        public Builder startsAt(LocalDateTime v) { this.startsAt = v; return this; }
        public Builder durationMinutes(int v) { this.durationMinutes = v; return this; }
        public Builder status(AppointmentStatus v) { this.status = v; return this; }
        public Builder complaint(String v) { this.complaint = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }

        public Appointment build() {
            Appointment a = new Appointment();
            a.doctor = this.doctor;
            a.patient = this.patient;
            a.startsAt = this.startsAt;
            a.durationMinutes = this.durationMinutes;
            a.status = this.status;
            a.complaint = this.complaint;
            a.notes = this.notes;
            return a;
        }
    }
}

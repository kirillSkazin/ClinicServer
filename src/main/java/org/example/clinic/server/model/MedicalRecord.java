package org.example.clinic.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(name = "medical_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_medical_records_appointment",
                columnNames = "appointment_id"))
public class MedicalRecord extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_medical_records_appointment"))
    private Appointment appointment;

    @Column(name = "diagnosis", nullable = false, length = 2048)
    private String diagnosis;

    @Column(name = "prescription", length = 4096)
    private String prescription;

    @Column(name = "recommendations", length = 4096)
    private String recommendations;

    public MedicalRecord() {
    }

    public MedicalRecord(Appointment appointment, String diagnosis,
                         String prescription, String recommendations) {
        this.appointment = appointment;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.recommendations = recommendations;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }
}

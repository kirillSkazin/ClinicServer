package org.example.clinic.server.dto;

import org.example.clinic.server.model.MedicalRecord;

import java.time.LocalDateTime;

public class MedicalRecordDto {
    private Long id;
    private Long appointmentId;
    private LocalDateTime appointmentDate;
    private String doctorName;
    private String patientName;
    private String specialization;
    private String diagnosis;
    private String prescription;
    private String recommendations;

    public static MedicalRecordDto from(MedicalRecord m) {
        MedicalRecordDto d = new MedicalRecordDto();
        d.id = m.getId();
        d.appointmentId = m.getAppointment().getId();
        d.appointmentDate = m.getAppointment().getStartsAt();
        d.doctorName = m.getAppointment().getDoctor().getUser().getFullName();
        d.patientName = m.getAppointment().getPatient().getUser().getFullName();
        d.specialization = m.getAppointment().getDoctor().getSpecialization().getName();
        d.diagnosis = m.getDiagnosis();
        d.prescription = m.getPrescription();
        d.recommendations = m.getRecommendations();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public LocalDateTime getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
}

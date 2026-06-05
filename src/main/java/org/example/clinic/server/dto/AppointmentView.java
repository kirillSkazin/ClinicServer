package org.example.clinic.server.dto;

import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;

import java.time.LocalDateTime;

public class AppointmentView {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String roomNumber;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private LocalDateTime startsAt;
    private int durationMinutes;
    private AppointmentStatus status;
    private String complaint;
    private String notes;

    public static AppointmentView from(Appointment a) {
        AppointmentView v = new AppointmentView();
        v.id = a.getId();
        v.doctorId = a.getDoctor().getId();
        v.doctorName = a.getDoctor().getUser().getFullName();
        v.specialization = a.getDoctor().getSpecialization().getName();
        v.roomNumber = a.getDoctor().getRoomNumber();
        v.patientId = a.getPatient().getId();
        v.patientName = a.getPatient().getUser().getFullName();
        v.patientPhone = a.getPatient().getUser().getPhone();
        v.startsAt = a.getStartsAt();
        v.durationMinutes = a.getDurationMinutes();
        v.status = a.getStatus();
        v.complaint = a.getComplaint();
        v.notes = a.getNotes();
        return v;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public String getComplaint() { return complaint; }
    public void setComplaint(String complaint) { this.complaint = complaint; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

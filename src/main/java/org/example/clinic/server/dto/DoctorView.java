package org.example.clinic.server.dto;

import org.example.clinic.server.model.Doctor;

public class DoctorView {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private Long specializationId;
    private String specializationName;
    private String roomNumber;
    private Integer experienceYears;
    private String bio;
    private boolean active;

    public DoctorView() {
    }

    public static DoctorView from(Doctor d) {
        DoctorView v = new DoctorView();
        v.id = d.getId();
        v.userId = d.getUser().getId();
        v.fullName = d.getUser().getFullName();
        v.email = d.getUser().getEmail();
        v.phone = d.getUser().getPhone();
        v.specializationId = d.getSpecialization().getId();
        v.specializationName = d.getSpecialization().getName();
        v.roomNumber = d.getRoomNumber();
        v.experienceYears = d.getExperienceYears();
        v.bio = d.getBio();
        v.active = d.getUser().isActive();
        return v;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }

    public String getSpecializationName() { return specializationName; }
    public void setSpecializationName(String specializationName) { this.specializationName = specializationName; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

package org.example.clinic.server.dto;

import org.example.clinic.server.model.Patient;

import java.time.LocalDate;

public class PatientView {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String address;
    private String insuranceNumber;

    public static PatientView from(Patient p) {
        PatientView v = new PatientView();
        v.id = p.getId();
        v.userId = p.getUser().getId();
        v.fullName = p.getUser().getFullName();
        v.email = p.getUser().getEmail();
        v.phone = p.getUser().getPhone();
        v.birthDate = p.getBirthDate();
        v.address = p.getAddress();
        v.insuranceNumber = p.getInsuranceNumber();
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

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getInsuranceNumber() { return insuranceNumber; }
    public void setInsuranceNumber(String insuranceNumber) { this.insuranceNumber = insuranceNumber; }
}

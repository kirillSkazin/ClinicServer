package org.example.clinic.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;


@Entity
@Table(name = "patients",
        uniqueConstraints = @UniqueConstraint(name = "uk_patients_user", columnNames = "user_id"))
public class Patient extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_patients_user"))
    private User user;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "insurance_number", length = 64)
    private String insuranceNumber;

    public Patient() {
    }

    public Patient(User user, LocalDate birthDate, String address, String insuranceNumber) {
        this.user = user;
        this.birthDate = birthDate;
        this.address = address;
        this.insuranceNumber = insuranceNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }
}

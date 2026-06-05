package org.example.clinic.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(name = "doctors",
        uniqueConstraints = @UniqueConstraint(name = "uk_doctors_user", columnNames = "user_id"))
public class Doctor extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_doctors_user"))
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_doctors_specialization"))
    private Specialization specialization;

    @Column(name = "room_number", length = 16)
    private String roomNumber;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "bio", length = 2048)
    private String bio;

    public Doctor() {
    }

    public Doctor(User user, Specialization specialization, String roomNumber,
                  Integer experienceYears, String bio) {
        this.user = user;
        this.specialization = specialization;
        this.roomNumber = roomNumber;
        this.experienceYears = experienceYears;
        this.bio = bio;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}

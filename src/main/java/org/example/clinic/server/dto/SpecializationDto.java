package org.example.clinic.server.dto;

import org.example.clinic.server.model.Specialization;

public class SpecializationDto {
    private Long id;
    private String name;
    private String description;

    public SpecializationDto() {
    }

    public SpecializationDto(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static SpecializationDto from(Specialization s) {
        return new SpecializationDto(s.getId(), s.getName(), s.getDescription());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

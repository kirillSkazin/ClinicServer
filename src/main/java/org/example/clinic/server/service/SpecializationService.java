package org.example.clinic.server.service;

import org.example.clinic.server.dao.SpecializationDao;
import org.example.clinic.server.dto.SpecializationDto;
import org.example.clinic.server.exception.ConflictException;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Specialization;

import java.util.List;

public class SpecializationService {

    private final SpecializationDao dao;

    public SpecializationService(SpecializationDao dao) {
        this.dao = dao;
    }

    public List<SpecializationDto> list() {
        return dao.findAll().stream()
                .map(SpecializationDto::from)
                .toList();
    }

    public SpecializationDto create(SpecializationDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("Название специализации обязательно");
        }
        if (dao.findByName(dto.getName()).isPresent()) {
            throw new ConflictException("Специализация с таким названием уже существует");
        }
        Specialization s = new Specialization(dto.getName().trim(), dto.getDescription());
        return SpecializationDto.from(dao.save(s));
    }

    public SpecializationDto update(SpecializationDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new ValidationException("ID специализации обязателен");
        }
        Specialization existing = dao.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Специализация не найдена: " + dto.getId()));
        if (dto.getName() != null && !dto.getName().isBlank()) {
            existing.setName(dto.getName().trim());
        }
        existing.setDescription(dto.getDescription());
        return SpecializationDto.from(dao.update(existing));
    }

    public void delete(Long id) {
        if (!dao.deleteById(id)) {
            throw new NotFoundException("Специализация не найдена: " + id);
        }
    }
}

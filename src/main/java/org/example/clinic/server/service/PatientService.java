package org.example.clinic.server.service;

import org.example.clinic.server.dao.PatientDao;
import org.example.clinic.server.dto.PatientView;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.model.Patient;

import java.util.List;

public class PatientService {

    private final PatientDao patientDao;

    public PatientService(PatientDao patientDao) {
        this.patientDao = patientDao;
    }

    public List<PatientView> list() {
        return patientDao.findAll().stream().map(PatientView::from).toList();
    }

    public PatientView getById(Long id) {
        return PatientView.from(patientDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Пациент не найден: " + id)));
    }

    public Patient requireByUserId(Long userId) {
        return patientDao.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль пациента не найден для пользователя: " + userId));
    }

    public PatientView getByUserId(Long userId) {
        return PatientView.from(requireByUserId(userId));
    }
}

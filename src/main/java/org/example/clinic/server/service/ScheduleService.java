package org.example.clinic.server.service;

import org.example.clinic.server.dao.AppointmentDao;
import org.example.clinic.server.dao.DoctorDao;
import org.example.clinic.server.dao.DoctorScheduleDao;
import org.example.clinic.server.dto.ScheduleEntryDto;
import org.example.clinic.server.dto.TimeSlotDto;
import org.example.clinic.server.exception.NotFoundException;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Appointment;
import org.example.clinic.server.model.AppointmentStatus;
import org.example.clinic.server.model.Doctor;
import org.example.clinic.server.model.DoctorSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class ScheduleService {

    private final DoctorScheduleDao scheduleDao;
    private final DoctorDao doctorDao;
    private final AppointmentDao appointmentDao;

    public ScheduleService(DoctorScheduleDao scheduleDao,
                           DoctorDao doctorDao,
                           AppointmentDao appointmentDao) {
        this.scheduleDao = scheduleDao;
        this.doctorDao = doctorDao;
        this.appointmentDao = appointmentDao;
    }

    public List<ScheduleEntryDto> getDoctorSchedule(Long doctorId) {
        return scheduleDao.findByDoctor(doctorId).stream()
                .map(ScheduleEntryDto::from).toList();
    }

    public ScheduleEntryDto upsertEntry(ScheduleEntryDto dto) {
        if (dto == null || dto.getDoctorId() == null
                || dto.getDayOfWeek() == null
                || dto.getStartTime() == null
                || dto.getEndTime() == null) {
            throw new ValidationException("Не заполнены обязательные поля расписания");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new ValidationException("Время окончания должно быть позже времени начала");
        }
        if (dto.getSlotMinutes() <= 0) {
            throw new ValidationException("Длительность слота должна быть положительной");
        }
        Doctor doctor = doctorDao.findById(dto.getDoctorId())
                .orElseThrow(() -> new NotFoundException("Врач не найден: " + dto.getDoctorId()));

        Optional<DoctorSchedule> existing = scheduleDao.findByDoctorAndDay(
                dto.getDoctorId(), dto.getDayOfWeek());
        DoctorSchedule entry = existing.orElseGet(DoctorSchedule::new);
        entry.setDoctor(doctor);
        entry.setDayOfWeek(dto.getDayOfWeek());
        entry.setStartTime(dto.getStartTime());
        entry.setEndTime(dto.getEndTime());
        entry.setSlotMinutes(dto.getSlotMinutes());

        DoctorSchedule saved = entry.getId() == null
                ? scheduleDao.save(entry)
                : scheduleDao.update(entry);
        return ScheduleEntryDto.from(saved);
    }

    public void deleteEntry(Long entryId) {
        if (!scheduleDao.deleteById(entryId)) {
            throw new NotFoundException("Запись расписания не найдена: " + entryId);
        }
    }

    

    public List<TimeSlotDto> getAvailableSlots(Long doctorId, LocalDate date) {
        if (date == null) {
            throw new ValidationException("Дата не указана");
        }
        DayOfWeek dow = date.getDayOfWeek();
        Optional<DoctorSchedule> schedule = scheduleDao.findByDoctorAndDay(doctorId, dow);
        if (schedule.isEmpty()) {
            return List.of();
        }
        DoctorSchedule sch = schedule.get();
        LocalDateTime cursor = LocalDateTime.of(date, sch.getStartTime());
        LocalDateTime endOfDay = LocalDateTime.of(date, sch.getEndTime());

        LocalDateTime fromBound = LocalDateTime.of(date, sch.getStartTime());
        LocalDateTime toBound = LocalDateTime.of(date, sch.getEndTime());

        List<Appointment> existing = appointmentDao.findByDoctor(doctorId, fromBound, toBound)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PLANNED
                        || a.getStatus() == AppointmentStatus.CONFIRMED)
                .toList();

        Set<LocalDateTime> busyStarts = new HashSet<>();
        for (Appointment a : existing) {
            busyStarts.add(a.getStartsAt());
        }

        LocalDateTime now = LocalDateTime.now();
        List<TimeSlotDto> slots = new ArrayList<>();
        while (!cursor.plusMinutes(sch.getSlotMinutes()).isAfter(endOfDay)) {
            boolean inPast = cursor.isBefore(now);
            boolean busy = busyStarts.contains(cursor);
            slots.add(new TimeSlotDto(cursor, sch.getSlotMinutes(), !busy && !inPast));
            cursor = cursor.plusMinutes(sch.getSlotMinutes());
        }
        return slots;
    }
}

package org.example.clinic.server.dto;

import org.example.clinic.server.model.DoctorSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public class ScheduleEntryDto {
    private Long id;
    private Long doctorId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotMinutes;

    public ScheduleEntryDto() {
    }

    public static ScheduleEntryDto from(DoctorSchedule s) {
        ScheduleEntryDto d = new ScheduleEntryDto();
        d.id = s.getId();
        d.doctorId = s.getDoctor().getId();
        d.dayOfWeek = s.getDayOfWeek();
        d.startTime = s.getStartTime();
        d.endTime = s.getEndTime();
        d.slotMinutes = s.getSlotMinutes();
        return d;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }
}

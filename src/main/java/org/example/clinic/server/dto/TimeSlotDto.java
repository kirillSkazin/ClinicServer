package org.example.clinic.server.dto;

import java.time.LocalDateTime;

public class TimeSlotDto {
    private LocalDateTime startsAt;
    private int durationMinutes;
    private boolean available;

    public TimeSlotDto() {
    }

    public TimeSlotDto(LocalDateTime startsAt, int durationMinutes, boolean available) {
        this.startsAt = startsAt;
        this.durationMinutes = durationMinutes;
        this.available = available;
    }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}

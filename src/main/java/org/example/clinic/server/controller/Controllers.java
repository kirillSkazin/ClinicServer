package org.example.clinic.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.clinic.server.dto.AddMedicalRecordRequest;
import org.example.clinic.server.dto.BookAppointmentRequest;
import org.example.clinic.server.dto.CreateDoctorRequest;
import org.example.clinic.server.dto.LoginRequest;
import org.example.clinic.server.dto.RegisterPatientRequest;
import org.example.clinic.server.dto.ScheduleEntryDto;
import org.example.clinic.server.dto.SpecializationDto;
import org.example.clinic.server.exception.ValidationException;
import org.example.clinic.server.model.Role;
import org.example.clinic.server.network.RequestDispatcher;
import org.example.clinic.server.network.protocol.JsonMapper;
import org.example.clinic.server.service.AppointmentService;
import org.example.clinic.server.service.AuthService;
import org.example.clinic.server.service.DoctorService;
import org.example.clinic.server.service.MedicalRecordService;
import org.example.clinic.server.service.PatientService;
import org.example.clinic.server.service.ScheduleService;
import org.example.clinic.server.service.SpecializationService;
import org.example.clinic.server.service.StatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;


public final class Controllers {

    private static final ObjectMapper MAPPER = JsonMapper.get();

    private Controllers() {
    }

    public static void registerAll(RequestDispatcher dispatcher,
                                   AuthService auth,
                                   DoctorService doctors,
                                   PatientService patients,
                                   SpecializationService specializations,
                                   AppointmentService appointments,
                                   MedicalRecordService records,
                                   ScheduleService schedules,
                                   StatisticsService statistics) {

        registerAuth(dispatcher, auth);
        registerSpecializations(dispatcher, specializations);
        registerDoctors(dispatcher, doctors);
        registerPatients(dispatcher, patients);
        registerSchedules(dispatcher, schedules);
        registerAppointments(dispatcher, appointments);
        registerMedicalRecords(dispatcher, records);
        registerStatistics(dispatcher, statistics);
    }

    private static void registerAuth(RequestDispatcher d, AuthService auth) {
        d.register("auth.login", false, null, (payload, principal) ->
                auth.login(parse(payload, LoginRequest.class)));
        d.register("auth.register-patient", false, null, (payload, principal) ->
                auth.registerPatient(parse(payload, RegisterPatientRequest.class)));
        d.register("auth.me", true, null, (payload, principal) -> Map.of(
                "userId", principal.userId(),
                "username", principal.username(),
                "role", principal.role()
        ));
    }

    private static void registerSpecializations(RequestDispatcher d, SpecializationService svc) {
        d.register("specializations.list", true, null,
                (payload, principal) -> svc.list());
        d.register("specializations.create", true, new Role[]{Role.ADMIN},
                (payload, principal) -> svc.create(parse(payload, SpecializationDto.class)));
        d.register("specializations.update", true, new Role[]{Role.ADMIN},
                (payload, principal) -> svc.update(parse(payload, SpecializationDto.class)));
        d.register("specializations.delete", true, new Role[]{Role.ADMIN},
                (payload, principal) -> {
                    svc.delete(requireLong(payload, "id"));
                    return Map.of("deleted", true);
                });
    }

    private static void registerDoctors(RequestDispatcher d, DoctorService svc) {
        d.register("doctors.list", true, null,
                (payload, principal) -> svc.list());
        d.register("doctors.search", true, null,
                (payload, principal) -> svc.searchByName(text(payload, "query")));
        d.register("doctors.by-specialization", true, null,
                (payload, principal) -> svc.findBySpecialization(requireLong(payload, "specializationId")));
        d.register("doctors.get", true, null,
                (payload, principal) -> svc.getById(requireLong(payload, "id")));
        d.register("doctors.create", true, new Role[]{Role.ADMIN},
                (payload, principal) -> svc.create(parse(payload, CreateDoctorRequest.class)));
        d.register("doctors.update", true, new Role[]{Role.ADMIN},
                (payload, principal) -> svc.update(
                        requireLong(payload, "id"),
                        parse(payload.path("data"), CreateDoctorRequest.class)));
        d.register("doctors.delete", true, new Role[]{Role.ADMIN},
                (payload, principal) -> {
                    svc.delete(requireLong(payload, "id"));
                    return Map.of("deleted", true);
                });
    }

    private static void registerPatients(RequestDispatcher d, PatientService svc) {
        d.register("patients.list", true, new Role[]{Role.ADMIN, Role.DOCTOR},
                (payload, principal) -> svc.list());
        d.register("patients.get", true, new Role[]{Role.ADMIN, Role.DOCTOR},
                (payload, principal) -> svc.getById(requireLong(payload, "id")));
        d.register("patients.me", true, new Role[]{Role.PATIENT},
                (payload, principal) -> svc.getByUserId(principal.userId()));
    }

    private static void registerSchedules(RequestDispatcher d, ScheduleService svc) {
        d.register("schedules.get", true, null,
                (payload, principal) -> svc.getDoctorSchedule(requireLong(payload, "doctorId")));
        d.register("schedules.upsert", true, new Role[]{Role.ADMIN, Role.DOCTOR},
                (payload, principal) -> svc.upsertEntry(parse(payload, ScheduleEntryDto.class)));
        d.register("schedules.delete", true, new Role[]{Role.ADMIN, Role.DOCTOR},
                (payload, principal) -> {
                    svc.deleteEntry(requireLong(payload, "id"));
                    return Map.of("deleted", true);
                });
        d.register("schedules.available-slots", true, null,
                (payload, principal) -> svc.getAvailableSlots(
                        requireLong(payload, "doctorId"),
                        LocalDate.parse(text(payload, "date"))));
    }

    private static void registerAppointments(RequestDispatcher d, AppointmentService svc) {
        d.register("appointments.book", true, new Role[]{Role.PATIENT},
                (payload, principal) -> svc.book(principal,
                        parse(payload, BookAppointmentRequest.class)));
        d.register("appointments.cancel", true, null,
                (payload, principal) -> svc.cancel(principal, requireLong(payload, "id")));
        d.register("appointments.confirm", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.confirm(principal, requireLong(payload, "id")));
        d.register("appointments.complete", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.complete(principal,
                        requireLong(payload, "id"), text(payload, "notes")));
        d.register("appointments.miss", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.markMissed(principal, requireLong(payload, "id")));
        d.register("appointments.reschedule", true, null,
                (payload, principal) -> svc.reschedule(principal,
                        requireLong(payload, "id"),
                        LocalDateTime.parse(text(payload, "startsAt")),
                        optInt(payload, "durationMinutes")));
        d.register("appointments.mine", true, null,
                (payload, principal) -> svc.myAppointments(principal));
        d.register("appointments.upcoming", true, new Role[]{Role.PATIENT},
                (payload, principal) -> svc.upcomingForPatient(principal));
        d.register("appointments.doctor-schedule", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.doctorSchedule(principal,
                        optLong(payload, "doctorId"),
                        optLocalDateTime(payload, "from"),
                        optLocalDateTime(payload, "to")));
    }

    private static void registerMedicalRecords(RequestDispatcher d, MedicalRecordService svc) {
        d.register("records.add", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.add(principal,
                        parse(payload, AddMedicalRecordRequest.class)));
        d.register("records.mine", true, new Role[]{Role.PATIENT},
                (payload, principal) -> svc.myHistory(principal));
        d.register("records.by-patient", true, new Role[]{Role.DOCTOR, Role.ADMIN},
                (payload, principal) -> svc.historyForPatient(principal,
                        requireLong(payload, "patientId")));
        d.register("records.my-issued", true, new Role[]{Role.DOCTOR},
                (payload, principal) -> svc.myIssuedRecords(principal));
    }

    private static void registerStatistics(RequestDispatcher d, StatisticsService svc) {
        d.register("statistics.summary", true, new Role[]{Role.ADMIN},
                (payload, principal) -> svc.getStats(principal));
    }

    

    private static <T> T parse(JsonNode node, Class<T> type) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            throw new ValidationException("Тело запроса отсутствует");
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (Exception ex) {
            throw new ValidationException("Невалидный payload: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private static <T> T parse(JsonNode node, TypeReference<T> typeRef) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            throw new ValidationException("Тело запроса отсутствует");
        }
        try {
            return MAPPER.readValue(MAPPER.treeAsTokens(node), typeRef);
        } catch (Exception ex) {
            throw new ValidationException("Невалидный payload: " + ex.getMessage());
        }
    }

    private static long requireLong(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            throw new ValidationException("Поле '" + field + "' обязательно");
        }
        return node.path(field).asLong();
    }

    private static Long optLong(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        return node.path(field).asLong();
    }

    private static Integer optInt(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        return node.path(field).asInt();
    }

    private static String text(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) return null;
        return v.asText();
    }

    private static LocalDateTime optLocalDateTime(JsonNode node, String field) {
        String s = text(node, field);
        return s == null || s.isBlank() ? null : LocalDateTime.parse(s);
    }
}

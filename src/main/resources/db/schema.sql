
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    email           VARCHAR(128) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(32),
    role            VARCHAR(32)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);
CREATE INDEX IF NOT EXISTS ix_users_role ON users(role);

CREATE TABLE IF NOT EXISTS specializations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ,
    CONSTRAINT uk_specializations_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS doctors (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    specialization_id BIGINT NOT NULL,
    room_number       VARCHAR(16),
    experience_years  INTEGER,
    bio               VARCHAR(2048),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT uk_doctors_user UNIQUE (user_id),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_doctors_specialization FOREIGN KEY (specialization_id)
        REFERENCES specializations(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS patients (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    birth_date        DATE,
    address           VARCHAR(512),
    insurance_number  VARCHAR(64),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT uk_patients_user UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS doctor_schedules (
    id            BIGSERIAL PRIMARY KEY,
    doctor_id     BIGINT NOT NULL,
    day_of_week   VARCHAR(16) NOT NULL,
    start_time    TIME NOT NULL,
    end_time      TIME NOT NULL,
    slot_minutes  INTEGER NOT NULL DEFAULT 30,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_doctor_schedules_doctor_day UNIQUE (doctor_id, day_of_week),
    CONSTRAINT fk_doctor_schedules_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appointments (
    id                BIGSERIAL PRIMARY KEY,
    doctor_id         BIGINT NOT NULL,
    patient_id        BIGINT NOT NULL,
    starts_at         TIMESTAMP NOT NULL,
    duration_minutes  INTEGER NOT NULL DEFAULT 30,
    status            VARCHAR(32) NOT NULL,
    complaint         VARCHAR(1024),
    notes             VARCHAR(2048),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors(id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS ix_appointments_doctor_time
    ON appointments(doctor_id, starts_at);
CREATE INDEX IF NOT EXISTS ix_appointments_patient_time
    ON appointments(patient_id, starts_at);
CREATE INDEX IF NOT EXISTS ix_appointments_status
    ON appointments(status);

CREATE TABLE IF NOT EXISTS medical_records (
    id              BIGSERIAL PRIMARY KEY,
    appointment_id  BIGINT NOT NULL,
    diagnosis       VARCHAR(2048) NOT NULL,
    prescription    VARCHAR(4096),
    recommendations VARCHAR(4096),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT uk_medical_records_appointment UNIQUE (appointment_id),
    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reminders (
    id              BIGSERIAL PRIMARY KEY,
    appointment_id  BIGINT NOT NULL,
    send_at         TIMESTAMP NOT NULL,
    status          VARCHAR(32) NOT NULL,
    channel         VARCHAR(32) NOT NULL,
    sent_at         TIMESTAMP,
    attempts        INTEGER NOT NULL DEFAULT 0,
    last_error      VARCHAR(1024),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_reminders_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS ix_reminders_send_time ON reminders(send_at);
CREATE INDEX IF NOT EXISTS ix_reminders_status    ON reminders(status);

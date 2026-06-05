
INSERT INTO specializations (name, description, created_at) VALUES
    ('Терапевт', 'Первичный приём, общая практика', NOW()),
    ('Кардиолог', 'Заболевания сердечно-сосудистой системы', NOW()),
    ('Хирург', 'Оперативное лечение', NOW()),
    ('Невролог', 'Заболевания нервной системы', NOW()),
    ('Педиатр', 'Диагностика и лечение детей', NOW())
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (username, password_hash, email, full_name, phone, role, active, created_at) VALUES
    ('doctor.ivanova', '__SEEDED_USER_PASSWORD_HASH__', 'ivanova@clinic.local', 'Иванова Анна Сергеевна', '+375 29 111-11-11', 'DOCTOR', TRUE, NOW()),
    ('doctor.petrov', '__SEEDED_USER_PASSWORD_HASH__', 'petrov@clinic.local', 'Петров Дмитрий Олегович', '+375 29 222-22-22', 'DOCTOR', TRUE, NOW()),
    ('doctor.smirnova', '__SEEDED_USER_PASSWORD_HASH__', 'smirnova@clinic.local', 'Смирнова Елена Викторовна', '+375 29 333-33-33', 'DOCTOR', TRUE, NOW()),
    ('doctor.kuznetsov', '__SEEDED_USER_PASSWORD_HASH__', 'kuznetsov@clinic.local', 'Кузнецов Алексей Игоревич', '+375 29 444-44-44', 'DOCTOR', TRUE, NOW()),
    ('doctor.sokolova', '__SEEDED_USER_PASSWORD_HASH__', 'sokolova@clinic.local', 'Соколова Мария Андреевна', '+375 29 555-55-55', 'DOCTOR', TRUE, NOW()),
    ('patient.demo', '__SEEDED_USER_PASSWORD_HASH__', 'patient.demo@clinic.local', 'Демидов Павел Андреевич', '+375 29 665-66-66', 'PATIENT', TRUE, NOW())
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    email = EXCLUDED.email,
    full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    role = EXCLUDED.role,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO patients (user_id, birth_date, address, insurance_number, created_at)
SELECT u.id, DATE '1990-05-15', 'г. Минск, ул. П. Бровки, 1', 'POLICY-0001', NOW()
FROM users u
WHERE u.username = 'patient.demo'
ON CONFLICT (user_id) DO UPDATE SET
    birth_date = EXCLUDED.birth_date,
    address = EXCLUDED.address,
    insurance_number = EXCLUDED.insurance_number,
    updated_at = NOW();

INSERT INTO doctors (user_id, specialization_id, room_number, experience_years, bio, created_at)
SELECT u.id, s.id, v.room_number, v.experience_years, v.bio, NOW()
FROM (
    VALUES
        ('doctor.ivanova', 'Терапевт', '101', 12, 'Врач общей практики, профилактические осмотры и первичная диагностика'),
        ('doctor.petrov', 'Кардиолог', '205', 15, 'Диагностика и лечение заболеваний сердечно-сосудистой системы'),
        ('doctor.smirnova', 'Невролог', '303', 10, 'Консультации по заболеваниям нервной системы и головным болям'),
        ('doctor.kuznetsov', 'Хирург', '112', 18, 'Амбулаторная хирургия и послеоперационное наблюдение'),
        ('doctor.sokolova', 'Педиатр', '214', 9, 'Приём детей, вакцинация, профилактические осмотры')
) AS v(username, specialization_name, room_number, experience_years, bio)
JOIN users u ON u.username = v.username
JOIN specializations s ON s.name = v.specialization_name
ON CONFLICT (user_id) DO UPDATE SET
    specialization_id = EXCLUDED.specialization_id,
    room_number = EXCLUDED.room_number,
    experience_years = EXCLUDED.experience_years,
    bio = EXCLUDED.bio,
    updated_at = NOW();

INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time, slot_minutes, created_at)
SELECT d.id, v.day_of_week, v.start_time::time, v.end_time::time, v.slot_minutes, NOW()
FROM (
    VALUES
        ('doctor.ivanova', 'MONDAY', '09:00', '15:00', 30),
        ('doctor.ivanova', 'TUESDAY', '09:00', '15:00', 30),
        ('doctor.ivanova', 'WEDNESDAY', '09:00', '15:00', 30),
        ('doctor.ivanova', 'THURSDAY', '09:00', '15:00', 30),
        ('doctor.ivanova', 'FRIDAY', '09:00', '14:00', 30),
        ('doctor.petrov', 'MONDAY', '10:00', '16:00', 30),
        ('doctor.petrov', 'TUESDAY', '10:00', '16:00', 30),
        ('doctor.petrov', 'WEDNESDAY', '10:00', '16:00', 30),
        ('doctor.petrov', 'THURSDAY', '10:00', '16:00', 30),
        ('doctor.petrov', 'FRIDAY', '10:00', '15:00', 30),
        ('doctor.smirnova', 'MONDAY', '08:30', '14:30', 30),
        ('doctor.smirnova', 'TUESDAY', '08:30', '14:30', 30),
        ('doctor.smirnova', 'WEDNESDAY', '08:30', '14:30', 30),
        ('doctor.smirnova', 'THURSDAY', '08:30', '14:30', 30),
        ('doctor.smirnova', 'FRIDAY', '08:30', '13:30', 30),
        ('doctor.kuznetsov', 'MONDAY', '12:00', '18:00', 30),
        ('doctor.kuznetsov', 'TUESDAY', '12:00', '18:00', 30),
        ('doctor.kuznetsov', 'WEDNESDAY', '12:00', '18:00', 30),
        ('doctor.kuznetsov', 'THURSDAY', '12:00', '18:00', 30),
        ('doctor.kuznetsov', 'FRIDAY', '12:00', '17:00', 30),
        ('doctor.sokolova', 'MONDAY', '09:30', '15:30', 30),
        ('doctor.sokolova', 'TUESDAY', '09:30', '15:30', 30),
        ('doctor.sokolova', 'WEDNESDAY', '09:30', '15:30', 30),
        ('doctor.sokolova', 'THURSDAY', '09:30', '15:30', 30),
        ('doctor.sokolova', 'FRIDAY', '09:30', '14:30', 30)
) AS v(username, day_of_week, start_time, end_time, slot_minutes)
JOIN users u ON u.username = v.username
JOIN doctors d ON d.user_id = u.id
ON CONFLICT (doctor_id, day_of_week) DO UPDATE SET
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    slot_minutes = EXCLUDED.slot_minutes,
    updated_at = NOW();

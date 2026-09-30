-- DEVELOPMENT / TEST DATABASE ONLY. Run this whole file in Supabase SQL Editor.
-- Requires the current migrations, phase 4 enrolment schema, and phase 18 catalogue.
-- If using phase 25 to reset scheduling, run that reset BEFORE this seed.
-- Then create the examination period with the SAME academic year/semester,
-- select these courses, generate/place exams, allocate students, and publish.
-- Do not rerun destructive catalogue/reset scripts as prerequisites.
--
-- Creates 40 students (20 MEE + 20 CEE) and 80 registrations.
-- Year 2 / semester 1 is a DEMO curriculum assumption, not an official curriculum.
-- Login: generated computer number; shared demo password: e1n2o3c4h5.
-- Reruns preserve existing accounts; conflicting cohort IDs/enrolments abort.
-- Seed rows are derived inline; no staging tables are created.
BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';
SELECT pg_advisory_xact_lock(741902, 1);

DO $$
BEGIN
    IF (SELECT count(*)
        FROM (VALUES
            ('ENG-BENG-ME', 'ENG-ME', 'MEE2101'),
            ('ENG-BENG-ME', 'ENG-ME', 'MEE2202'),
            ('ENG-BENG-CEE', 'ENG-CEE', 'CEE2101'),
            ('ENG-BENG-CEE', 'ENG-CEE', 'CEE2202')
        ) AS mapping(programme_code, major_code, course_code)
        JOIN public.programme p
          ON p.programme_code = mapping.programme_code AND p.is_active
        JOIN public.school s ON s.school_id = p.school_id AND s.is_active
        JOIN public.major m
          ON m.major_code = mapping.major_code
         AND m.programme_id = p.programme_id AND m.is_active
        JOIN public.course c ON c.course_code = mapping.course_code AND c.is_active) <> 4 THEN
        RAISE EXCEPTION 'Missing/inactive MEE or CEE catalogue entries: check phase 18 programmes, majors and four courses';
    END IF;

    IF EXISTS (
        WITH settings AS (
            SELECT '2025/2026'::varchar(9) AS academic_year,
                   2::smallint AS year_of_study,
                   20::integer AS students_per_programme
        ),
        programme_majors AS (
            SELECT DISTINCT programme_code, major_code
            FROM (VALUES
                ('ENG-BENG-ME', 'ENG-ME'),
                ('ENG-BENG-CEE', 'ENG-CEE')
            ) AS mapping(programme_code, major_code)
        ),
        candidates AS (
            SELECT p.programme_id, m.major_id, p.programme_name,
                   cfg.academic_year, cfg.year_of_study,
                   (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN '2026901' ELSE '2026902' END)
                       || lpad(n::text, 3, '0') AS computer_number
            FROM programme_majors x
            JOIN public.programme p ON p.programme_code = x.programme_code
            JOIN public.major m ON m.major_code = x.major_code AND m.programme_id = p.programme_id
            CROSS JOIN settings cfg
            CROSS JOIN LATERAL generate_series(1, cfg.students_per_programme) n
        )
        SELECT 1
        FROM candidates x
        WHERE EXISTS (
            SELECT 1 FROM public.student s
            WHERE s.computer_number = x.computer_number
              AND (s.national_id IS DISTINCT FROM 'DEMO-' || x.computer_number
                   OR s.qr_token IS DISTINCT FROM 'phase26-' || x.computer_number
                   OR s.program IS DISTINCT FROM x.programme_name
                   OR s.year_of_study IS DISTINCT FROM x.year_of_study)
        ) OR EXISTS (
            SELECT 1 FROM public.student s
            WHERE s.computer_number <> x.computer_number
              AND (s.national_id = 'DEMO-' || x.computer_number
                   OR s.email = x.computer_number || '@example.invalid')
        ) OR EXISTS (
            SELECT 1 FROM public.student_programme_enrolment e
            WHERE e.computer_number = x.computer_number
              AND e.academic_year = x.academic_year
              AND (e.programme_id IS DISTINCT FROM x.programme_id
                   OR e.major_id IS DISTINCT FROM x.major_id
                   OR e.year_of_study IS DISTINCT FROM x.year_of_study
                   OR e.enrolment_status <> 'ACTIVE')
        )
    ) THEN
        RAISE EXCEPTION 'A generated student identity or programme enrolment conflicts with existing data';
    END IF;
END $$;

-- 1. Make each course available under its matching programme and major.
INSERT INTO public.programme_course
    (programme_id, major_id, course_code, year_of_study, semester, course_category, is_active)
SELECT p.programme_id, m.major_id, mapping.course_code, 2, 1, 'COMPULSORY', TRUE
FROM (VALUES
    ('ENG-BENG-ME', 'ENG-ME', 'MEE2101'),
    ('ENG-BENG-ME', 'ENG-ME', 'MEE2202'),
    ('ENG-BENG-CEE', 'ENG-CEE', 'CEE2101'),
    ('ENG-BENG-CEE', 'ENG-CEE', 'CEE2202')
) AS mapping(programme_code, major_code, course_code)
JOIN public.programme p ON p.programme_code = mapping.programme_code
JOIN public.major m ON m.major_code = mapping.major_code AND m.programme_id = p.programme_id
ON CONFLICT (programme_id, major_id, course_code, year_of_study, semester)
DO UPDATE SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP;

-- 2. Create students directly in the student table.
WITH settings AS (
    SELECT 20::integer AS students_per_programme, 2::smallint AS year_of_study
),
programme_majors AS (
    SELECT DISTINCT programme_code, major_code
    FROM (VALUES
        ('ENG-BENG-ME', 'ENG-ME'),
        ('ENG-BENG-CEE', 'ENG-CEE')
    ) AS mapping(programme_code, major_code)
),
candidates AS (
    SELECT p.programme_name, s.school_name, cfg.year_of_study,
           (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN '2026901' ELSE '2026902' END)
               || lpad(n::text, 3, '0') AS computer_number,
           (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN 'MEE' ELSE 'CEE' END)
               || ' Demo Student ' || lpad(n::text, 3, '0') AS full_name
    FROM programme_majors x
    JOIN public.programme p ON p.programme_code = x.programme_code
    JOIN public.school s ON s.school_id = p.school_id
    CROSS JOIN settings cfg
    CROSS JOIN LATERAL generate_series(1, cfg.students_per_programme) n
)
INSERT INTO public.student (
    computer_number, national_id, full_name, program, school, year_of_study,
    email, photo_path, qr_token, status, password_hash,
    account_activated, activated_at, account_status
)
SELECT computer_number, 'DEMO-' || computer_number, full_name, programme_name,
       school_name, year_of_study, computer_number || '@example.invalid',
       '/images/demo-placeholder.png', 'phase26-' || computer_number, 'ACTIVE',
       '$2b$12$2yiUpkmPAZrqWQXknWL2lu.q7eJ3D3RMuQ1Ultk0dlmbFlZcY.',
       TRUE, CURRENT_TIMESTAMP, 'ACTIVE'
FROM candidates
ON CONFLICT (computer_number) DO NOTHING;

-- 3. Enrol each student in the correct programme, major and academic year.
WITH settings AS (
    SELECT '2025/2026'::varchar(9) AS academic_year,
           20::integer AS students_per_programme, 2::smallint AS year_of_study
),
programme_majors AS (
    SELECT DISTINCT programme_code, major_code
    FROM (VALUES
        ('ENG-BENG-ME', 'ENG-ME'),
        ('ENG-BENG-CEE', 'ENG-CEE')
    ) AS mapping(programme_code, major_code)
),
candidates AS (
    SELECT p.programme_id, m.major_id, cfg.academic_year, cfg.year_of_study,
           (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN '2026901' ELSE '2026902' END)
               || lpad(n::text, 3, '0') AS computer_number
    FROM programme_majors x
    JOIN public.programme p ON p.programme_code = x.programme_code
    JOIN public.major m ON m.major_code = x.major_code AND m.programme_id = p.programme_id
    CROSS JOIN settings cfg
    CROSS JOIN LATERAL generate_series(1, cfg.students_per_programme) n
)
INSERT INTO public.student_programme_enrolment
    (computer_number, programme_id, major_id, academic_year, year_of_study, enrolment_status)
SELECT computer_number, programme_id, major_id, academic_year, year_of_study, 'ACTIVE'
FROM candidates
ON CONFLICT (computer_number, academic_year) DO NOTHING;

-- 4. Register each cohort for the two courses in its own programme.
-- Existing registrations are filtered out before INSERT to respect published-cycle guards.
WITH settings AS (
    SELECT '2025/2026'::varchar(9) AS academic_year,
           1::smallint AS semester, 20::integer AS students_per_programme
),
course_map AS (
    SELECT * FROM (VALUES
        ('ENG-BENG-ME', 'MEE2101'),
        ('ENG-BENG-ME', 'MEE2202'),
        ('ENG-BENG-CEE', 'CEE2101'),
        ('ENG-BENG-CEE', 'CEE2202')
    ) AS mapping(programme_code, course_code)
),
candidates AS (
    SELECT p.programme_code, cfg.academic_year, cfg.semester,
           (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN '2026901' ELSE '2026902' END)
               || lpad(n::text, 3, '0') AS computer_number
    FROM (SELECT DISTINCT programme_code FROM course_map) x
    JOIN public.programme p ON p.programme_code = x.programme_code
    CROSS JOIN settings cfg
    CROSS JOIN LATERAL generate_series(1, cfg.students_per_programme) n
)
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT x.computer_number, mapping.course_code, x.academic_year, x.semester
FROM candidates x
JOIN course_map mapping ON mapping.programme_code = x.programme_code
WHERE NOT EXISTS (
    SELECT 1 FROM public.student_registration r
    WHERE r.computer_number = x.computer_number
      AND r.course_code = mapping.course_code
      AND r.academic_year = x.academic_year
      AND r.semester = x.semester
)
ON CONFLICT (computer_number, course_code, academic_year, semester) DO NOTHING;

-- Verification: returns four rows, each with 20 seeded students.
WITH settings AS (
    SELECT '2025/2026'::varchar(9) AS academic_year,
           1::smallint AS semester, 20::integer AS students_per_programme
),
course_map AS (
    SELECT * FROM (VALUES
        ('ENG-BENG-ME', 'MEE2101'),
        ('ENG-BENG-ME', 'MEE2202'),
        ('ENG-BENG-CEE', 'CEE2101'),
        ('ENG-BENG-CEE', 'CEE2202')
    ) AS mapping(programme_code, course_code)
),
candidates AS (
    SELECT p.programme_code, cfg.academic_year, cfg.semester,
           (CASE WHEN p.programme_code = 'ENG-BENG-ME' THEN '2026901' ELSE '2026902' END)
               || lpad(n::text, 3, '0') AS computer_number
    FROM (SELECT DISTINCT programme_code FROM course_map) x
    JOIN public.programme p ON p.programme_code = x.programme_code
    CROSS JOIN settings cfg
    CROSS JOIN LATERAL generate_series(1, cfg.students_per_programme) n
)
SELECT r.course_code, r.academic_year, r.semester, count(*) AS seeded_registered_students
FROM candidates x
JOIN course_map mapping ON mapping.programme_code = x.programme_code
JOIN public.student_registration r
  ON r.computer_number = x.computer_number
 AND r.course_code = mapping.course_code
 AND r.academic_year = x.academic_year
 AND r.semester = x.semester
GROUP BY r.course_code, r.academic_year, r.semester
ORDER BY r.course_code;

COMMIT;

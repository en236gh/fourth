-- Phase 22: connect enrolled students to curriculum, exams, venues, and seats.
-- Run after phases 18, 20, and 21, plus V27/V30 academic schema migrations.
-- This seeds only the demo programme curricula represented by current students.
BEGIN;

-- Demo curricula: programme -> major -> course -> year -> semester.
INSERT INTO public.programme_course (
    programme_id,
    major_id,
    course_code,
    year_of_study,
    semester,
    course_category,
    is_active
)
SELECT
    p.programme_id,
    m.major_id,
    c.course_code,
    c.year_of_study,
    1,
    'COMPULSORY',
    TRUE
FROM (VALUES
    ('SNS-BCS', 'SNS-CS', 'CSC1101', 1),
    ('SNS-BCS', 'SNS-CS', 'CSC1202', 1),
    ('SNS-BCS', 'SNS-CS', 'MAT1101', 1),
    ('SNS-BCS', 'SNS-CS', 'CSC2201', 2),
    ('SNS-BCS', 'SNS-CS', 'CSC2302', 2),
    ('SNS-BCS', 'SNS-CS', 'CSC3101', 3),
    ('SNS-BCS', 'SNS-CS', 'CSC3202', 3),
    ('SNS-BCS', 'SNS-CS', 'CSC3303', 4),
    ('SNS-BCS', 'SNS-CS', 'CSC4101', 4),
    ('ENG-BENG-EEE', 'ENG-EMP', 'ENG1101', 1),
    ('ENG-BENG-EEE', 'ENG-EMP', 'ENG1202', 1),
    ('ENG-BENG-EEE', 'ENG-EMP', 'EEE2101', 2),
    ('ENG-BENG-EEE', 'ENG-EMP', 'EEE2202', 2),
    ('EDU-BSC-ED', 'EDU-MAT', 'EDU1101', 1),
    ('EDU-BSC-ED', 'EDU-MAT', 'EDU1202', 1),
    ('EDU-BSC-ED', 'EDU-MAT', 'MAT1101', 1),
    ('EDU-BSC-ED', 'EDU-MAT', 'EDU2101', 2),
    ('EDU-BSC-ED', 'EDU-MAT', 'EDU2202', 2),
    ('EDU-BSC-ED', 'EDU-MAT', 'MAT2101', 2)
) AS c(programme_code, major_code, course_code, year_of_study)
JOIN public.programme p ON p.programme_code = c.programme_code
JOIN public.major m ON m.major_code = c.major_code AND m.programme_id = p.programme_id
JOIN public.course catalog ON catalog.course_code = c.course_code AND catalog.is_active
ON CONFLICT (programme_id, major_id, course_code, year_of_study, semester) DO UPDATE
SET course_category = EXCLUDED.course_category,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- Register each enrolled student for every course in their programme curriculum.
-- Remove current-period registrations that do not belong to the student's
-- current year, so rerunning this seed enforces the year-specific rule.
DELETE FROM public.student_registration sr
WHERE EXISTS (
        SELECT 1
        FROM public.student_programme_enrolment e
        WHERE e.computer_number = sr.computer_number
            AND e.academic_year = sr.academic_year
            AND e.enrolment_status IN ('ACTIVE', 'DEFERRED')
            AND NOT EXISTS (
                    SELECT 1
                    FROM public.programme_course pc
                    WHERE pc.programme_id = e.programme_id
                        AND pc.course_code = sr.course_code
                        AND pc.semester = sr.semester
                        AND pc.year_of_study = e.year_of_study
                        AND pc.is_active
            )
);

INSERT INTO public.student_registration (
    computer_number,
    course_code,
    academic_year,
    semester
)
SELECT DISTINCT
    e.computer_number,
    pc.course_code,
    e.academic_year,
    pc.semester
FROM public.student_programme_enrolment e
JOIN public.programme_course pc ON pc.programme_id = e.programme_id
WHERE e.enrolment_status IN ('ACTIVE', 'DEFERRED')
  AND pc.is_active
    AND pc.year_of_study = e.year_of_study
ON CONFLICT DO NOTHING;

-- Link all sessions to their curriculum rows.
INSERT INTO public.exam_session_programme_course (exam_session_id, programme_course_id)
SELECT es.exam_session_id, pc.programme_course_id
FROM public.exam_session es
JOIN public.programme_course pc
    ON pc.course_code = es.course_code
   AND pc.semester = es.semester
   AND pc.is_active
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
ON CONFLICT DO NOTHING;

-- Attach sessions to existing venues by course family.
INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = CASE
    WHEN es.course_code LIKE 'CSC%' OR es.course_code LIKE 'MAT%' THEN 'Michael J. Kelly Lecture Theatre (formerly NELT)'
    WHEN es.course_code LIKE 'ENG%' OR es.course_code LIKE 'EEE%' THEN 'School of Engineering LT 1'
    WHEN es.course_code LIKE 'EDU%' THEN 'Special Needs Education Centre Auditorium'
    WHEN es.course_code LIKE 'CEE%' OR es.course_code LIKE 'MEE%' THEN 'School of Engineering LT 2'
END
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
ON CONFLICT DO NOTHING;

-- Allocate registered students to the venue assigned to each exam.
WITH ranked AS (
    SELECT
        sr.computer_number,
        es.exam_session_id,
        ev.venue_id
    FROM public.student_registration sr
    JOIN public.exam_session es
      ON es.course_code = sr.course_code
     AND es.academic_year = sr.academic_year
     AND es.semester = sr.semester
    JOIN public.exam_venue ev ON ev.exam_session_id = es.exam_session_id
    WHERE sr.academic_year = '2026/2027'
      AND sr.semester = 1
      AND es.exam_type = 'FINAL'
)
INSERT INTO public.student_venue_allocation (
    computer_number,
    exam_session_id,
    venue_id
)
SELECT
    computer_number,
    exam_session_id,
    venue_id
FROM ranked
ON CONFLICT (computer_number, exam_session_id) DO UPDATE
SET venue_id = EXCLUDED.venue_id;

COMMIT;

-- Verification of the complete student-to-exam chain.
SELECT
    e.computer_number,
    e.academic_year,
    e.year_of_study,
    e.programme_id,
    COUNT(DISTINCT sr.course_code) AS registered_courses,
    COUNT(DISTINCT sva.exam_session_id) AS allocated_exams
FROM public.student_programme_enrolment e
LEFT JOIN public.student_registration sr
    ON sr.computer_number = e.computer_number
   AND sr.academic_year = e.academic_year
LEFT JOIN public.student_venue_allocation sva
    ON sva.computer_number = e.computer_number
GROUP BY e.computer_number, e.academic_year, e.year_of_study, e.programme_id
ORDER BY e.computer_number;

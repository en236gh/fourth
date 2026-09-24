-- Phase 12: Seed demo registrations and student venue assignments.
-- Run after phases 9, 10, and 11.
-- The demo students are registered by programme family so every seeded
-- examination session has students for lecturer and administrator testing.

BEGIN;

-- Seed course registrations. The student table remains the source of identity;
-- registrations describe the courses each demo student is taking.
INSERT INTO public.student_registration (
    computer_number, course_code, academic_year, semester
)
SELECT students.computer_number, courses.course_code, '2026/2027', 1
FROM (
    VALUES
        ('2022004264'), ('2022004265'), ('2022004266'),
        ('2022004272'), ('2022004273'), ('2022004280'), ('2022004281')
) AS students(computer_number)
CROSS JOIN (
    VALUES
        ('CSC1101'), ('CSC1202'), ('CSC2201'), ('CSC2302'),
        ('CSC3101'), ('CSC3202'), ('CSC3303'), ('CSC4101'),
        ('MAT1101'), ('MAT2101')
) AS courses(course_code)
JOIN public.student s ON s.computer_number = students.computer_number
ON CONFLICT DO NOTHING;

INSERT INTO public.student_registration (
    computer_number, course_code, academic_year, semester
)
SELECT students.computer_number, courses.course_code, '2026/2027', 1
FROM (
    VALUES
        ('2022004267'), ('2022004268'), ('2022004274'),
        ('2022004275'), ('2022004282')
) AS students(computer_number)
CROSS JOIN (
    VALUES
        ('ENG1101'), ('ENG1202'), ('EEE2101'), ('EEE2202'),
        ('CEE2101'), ('CEE2202'), ('MEE2101'), ('MEE2202')
) AS courses(course_code)
JOIN public.student s ON s.computer_number = students.computer_number
ON CONFLICT DO NOTHING;

INSERT INTO public.student_registration (
    computer_number, course_code, academic_year, semester
)
SELECT students.computer_number, courses.course_code, '2026/2027', 1
FROM (
    VALUES
        ('2022004269'), ('2022004270'), ('2022004276'),
        ('2022004277'), ('2022004283')
) AS students(computer_number)
CROSS JOIN (
    VALUES
        ('EDU1101'), ('EDU1202'), ('EDU2101'), ('EDU2202'),
        ('MAT1101'), ('MAT2101')
) AS courses(course_code)
JOIN public.student s ON s.computer_number = students.computer_number
ON CONFLICT DO NOTHING;

-- Allocate registered students to the venues linked to each exam session.
-- Existing allocations for the same student/session are refreshed so this
-- seed can be run again after changing registrations or venue links.
WITH venue_order AS (
    SELECT
        es.exam_session_id,
        es.course_code,
        ev.venue_id,
        v.capacity,
        COALESCE(
            SUM(v.capacity) OVER (
                PARTITION BY es.exam_session_id
                ORDER BY ev.venue_id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS capacity_before
    FROM public.exam_session es
    JOIN public.exam_venue ev ON ev.exam_session_id = es.exam_session_id
    JOIN public.venue v ON v.venue_id = ev.venue_id
    WHERE es.academic_year = '2026/2027'
      AND es.semester = 1
      AND es.exam_type = 'FINAL'
),
ranked_students AS (
    SELECT
        sr.computer_number,
        es.exam_session_id,
        ROW_NUMBER() OVER (
            PARTITION BY es.exam_session_id
            ORDER BY sr.computer_number
        ) AS student_position
    FROM public.student_registration sr
    JOIN public.exam_session es ON es.course_code = sr.course_code
    WHERE sr.academic_year = '2026/2027'
      AND sr.semester = 1
      AND es.academic_year = sr.academic_year
      AND es.semester = sr.semester
      AND es.exam_type = 'FINAL'
),
assigned_students AS (
    SELECT
        ranked.computer_number,
        ranked.exam_session_id,
        venues.venue_id
    FROM ranked_students ranked
    JOIN venue_order venues
      ON venues.exam_session_id = ranked.exam_session_id
     AND ranked.student_position > venues.capacity_before
     AND ranked.student_position <= venues.capacity_before + venues.capacity
)
INSERT INTO public.student_venue_allocation (
    computer_number, exam_session_id, venue_id
)
SELECT
    computer_number,
    exam_session_id,
    venue_id
FROM assigned_students
ON CONFLICT (computer_number, exam_session_id) DO UPDATE
SET venue_id = EXCLUDED.venue_id;

COMMIT;

-- Verification:
-- SELECT es.exam_session_id, es.course_code, COUNT(sva.*) AS allocated_students
-- FROM public.exam_session es
-- LEFT JOIN public.student_venue_allocation sva
--   ON sva.exam_session_id = es.exam_session_id
-- WHERE es.academic_year = '2026/2027'
-- GROUP BY es.exam_session_id, es.course_code
-- ORDER BY es.exam_session_id;

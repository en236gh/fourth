-- Phase 9: Seed examination sessions for every course in Phase 3.
-- Run after phase_3_seed_academic_catalog.sql and phase_4_connect_operational_academics.sql.
-- The script is safe to run repeatedly for the same course/date combination.

BEGIN;

INSERT INTO public.exam_session (
    course_code,
    exam_date,
    start_time,
    end_time,
    academic_year,
    semester,
    exam_type
)
SELECT
    e.course_code,
    e.exam_date,
    e.start_time,
    e.end_time,
    e.academic_year,
    e.semester,
    e.exam_type
FROM (
    VALUES
        ('CSC1101', DATE '2026-10-05', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CSC1202', DATE '2026-10-05', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('MAT1101', DATE '2026-10-06', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('ENG1101', DATE '2026-10-06', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('ENG1202', DATE '2026-10-07', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('EDU1101', DATE '2026-10-07', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('EDU1202', DATE '2026-10-08', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CSC2201', DATE '2026-10-08', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('EEE2101', DATE '2026-10-09', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CEE2101', DATE '2026-10-09', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('MEE2101', DATE '2026-10-12', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CSC2302', DATE '2026-10-12', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('EEE2202', DATE '2026-10-13', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CEE2202', DATE '2026-10-13', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('MEE2202', DATE '2026-10-14', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('EDU2101', DATE '2026-10-14', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('EDU2202', DATE '2026-10-15', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('MAT2101', DATE '2026-10-15', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('CSC3101', DATE '2026-10-16', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CSC3202', DATE '2026-10-16', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL'),
        ('CSC3303', DATE '2026-10-19', TIME '09:00', TIME '11:00', '2026/2027', 1, 'FINAL'),
        ('CSC4101', DATE '2026-10-19', TIME '13:00', TIME '15:00', '2026/2027', 1, 'FINAL')
) AS e(course_code, exam_date, start_time, end_time, academic_year, semester, exam_type)
JOIN public.course c ON c.course_code = e.course_code
WHERE NOT EXISTS (
    SELECT 1
    FROM public.exam_session existing
    WHERE existing.course_code = e.course_code
      AND existing.exam_date = e.exam_date
      AND existing.start_time = e.start_time
      AND existing.academic_year = e.academic_year
      AND existing.semester = e.semester
);

-- Link each session to all matching curriculum entries for semester 1.
INSERT INTO public.exam_session_programme_course (exam_session_id, programme_course_id)
SELECT es.exam_session_id, pc.programme_course_id
FROM public.exam_session es
JOIN public.programme_course pc
    ON pc.course_code = es.course_code
   AND pc.semester = es.semester
   AND pc.is_active = TRUE
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
  AND es.exam_date >= DATE '2026-10-05'
  AND es.exam_date <= DATE '2026-10-19'
ON CONFLICT (exam_session_id, programme_course_id) DO NOTHING;

COMMIT;

-- Check the inserted schedule:
-- SELECT es.course_code, c.course_name, es.exam_date, es.start_time, es.end_time
-- FROM public.exam_session es
-- JOIN public.course c ON c.course_code = es.course_code
-- WHERE es.academic_year = '2026/2027'
-- ORDER BY es.exam_date, es.start_time;
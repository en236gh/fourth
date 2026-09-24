-- Phase 21: seed examination sessions for the current course catalogue.
-- Run after phase_18_replace_academic_catalog.sql and the academic schema migrations.
-- Sessions use academic year 2026/2027, semester 1.
-- The programme-course links are added automatically when curriculum rows exist.
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
FROM (VALUES
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
JOIN public.course c ON c.course_code = e.course_code AND c.is_active
WHERE NOT EXISTS (
    SELECT 1
    FROM public.exam_session existing
    WHERE existing.course_code = e.course_code
      AND existing.exam_date = e.exam_date
      AND existing.start_time = e.start_time
      AND existing.academic_year = e.academic_year
      AND existing.semester = e.semester
);

-- Connect each exam to all matching programme/major curriculum rows.
INSERT INTO public.exam_session_programme_course (exam_session_id, programme_course_id)
SELECT es.exam_session_id, pc.programme_course_id
FROM public.exam_session es
JOIN public.programme_course pc
    ON pc.course_code = es.course_code
   AND pc.semester = es.semester
   AND pc.is_active
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
ON CONFLICT (exam_session_id, programme_course_id) DO NOTHING;

COMMIT;

-- Verification
SELECT es.exam_session_id,
       es.course_code,
       c.course_name,
       es.exam_date,
       es.start_time,
       es.end_time,
       es.academic_year,
       es.semester,
       COUNT(espc.programme_course_id) AS curriculum_links
FROM public.exam_session es
JOIN public.course c ON c.course_code = es.course_code
LEFT JOIN public.exam_session_programme_course espc
    ON espc.exam_session_id = es.exam_session_id
WHERE es.academic_year = '2026/2027'
  AND es.semester = 1
GROUP BY es.exam_session_id, c.course_name
ORDER BY es.exam_date, es.start_time;

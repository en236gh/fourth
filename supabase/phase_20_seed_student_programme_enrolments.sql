-- Phase 20: link existing students to the new programme catalogue.
-- Run after phase_19_reset_academic_exam_data.sql and phase_18_replace_academic_catalog.sql.
-- The legacy student.program values do not identify an exact major, so major_id
-- remains NULL until student-major assignments are confirmed.
BEGIN;

-- Demo students use the legacy programme labels below:
-- Computer Science       -> SNS-BCS
-- Electrical Engineering -> ENG-BENG-EEE
-- Education              -> EDU-BSC-ED
INSERT INTO public.student_programme_enrolment (
    computer_number,
    programme_id,
    academic_year,
    year_of_study,
    enrolment_status
)
SELECT
    s.computer_number,
    p.programme_id,
    COALESCE(
        (SELECT MIN(sr.academic_year)
         FROM public.student_registration sr
         WHERE sr.computer_number = s.computer_number),
        '2026/2027'
    ),
    s.year_of_study,
    CASE
        WHEN s.status = 'SUSPENDED' THEN 'SUSPENDED'
        WHEN s.status = 'DEFERRED' THEN 'DEFERRED'
        WHEN s.status = 'GRADUATED' THEN 'COMPLETED'
        ELSE 'ACTIVE'
    END
FROM public.student s
JOIN public.programme p
    ON p.programme_code = CASE
        WHEN s.program = 'Computer Science' THEN 'SNS-BCS'
        WHEN s.program = 'Electrical Engineering' THEN 'ENG-BENG-EEE'
        WHEN s.program = 'Education' THEN 'EDU-BSC-ED'
    END
WHERE s.program IN ('Computer Science', 'Electrical Engineering', 'Education')
ON CONFLICT (computer_number, academic_year) DO UPDATE
SET programme_id = EXCLUDED.programme_id,
    year_of_study = EXCLUDED.year_of_study,
    enrolment_status = EXCLUDED.enrolment_status,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

-- Verification
SELECT e.computer_number,
       s.full_name,
       p.programme_code,
       p.programme_name,
       e.academic_year,
       e.year_of_study,
       e.enrolment_status
FROM public.student_programme_enrolment e
JOIN public.student s ON s.computer_number = e.computer_number
JOIN public.programme p ON p.programme_id = e.programme_id
WHERE e.academic_year IN ('2026', '2026/2027', '2025/2026')
ORDER BY p.programme_code, e.year_of_study, e.computer_number;

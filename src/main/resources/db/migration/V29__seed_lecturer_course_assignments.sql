-- Run after V27__seed_requested_staff_accounts.sql and V28__seed_additional_lecturers.sql.
-- Assign lecturers to existing courses from supabase/phase_3_seed_academic_catalog.sql.
-- This script only inserts course_lecturer links; it does not create or change courses.

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM public.staff WHERE email IN (
        'lecturer@gmail.com', 'lecturer2@gmail.com', 'lecturer3@gmail.com',
        'lecturer4@gmail.com', 'lecturer5@gmail.com'
    )) <> 5 THEN
        RAISE EXCEPTION 'Seed all five lecturer accounts before assigning courses';
    END IF;
    IF (SELECT COUNT(*) FROM public.course WHERE course_code IN (
        'CSC1101', 'CSC1202', 'CSC2201', 'CSC2302', 'CSC3101'
    )) <> 5 THEN
        RAISE EXCEPTION 'Expected existing catalog courses CSC1101, CSC1202, CSC2201, CSC2302, CSC3101; check the course codes before assigning lecturers';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM (VALUES ('CSC1101'), ('CSC1202'), ('CSC2201'), ('CSC2302'), ('CSC3101')) AS expected(course_code)
        WHERE NOT EXISTS (
            SELECT 1
            FROM public.programme_course pc
            JOIN public.programme p ON p.programme_id = pc.programme_id
            JOIN public.school sch ON sch.school_id = p.school_id
            JOIN public.course c ON c.course_code = pc.course_code
            WHERE pc.course_code = expected.course_code
              AND pc.is_active AND p.is_active AND sch.is_active AND c.is_active
        )
    ) THEN
        RAISE EXCEPTION 'Each lecturer course must have an existing active school, programme and curriculum entry; check the academic catalog mappings';
    END IF;
END $$;

INSERT INTO public.course_lecturer (course_code, staff_id)
SELECT assignment.course_code, s.staff_id
FROM (VALUES
    ('lecturer@gmail.com', 'CSC1101'),
    ('lecturer2@gmail.com', 'CSC1202'),
    ('lecturer3@gmail.com', 'CSC2201'),
    ('lecturer4@gmail.com', 'CSC2302'),
    ('lecturer5@gmail.com', 'CSC3101')
) AS assignment(email, course_code)
JOIN public.staff s ON s.email = assignment.email
JOIN public.course c ON c.course_code = assignment.course_code
ON CONFLICT (course_code, staff_id) DO NOTHING;

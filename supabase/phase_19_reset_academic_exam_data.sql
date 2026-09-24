-- Phase 19: destructive reset of academic and examination data.
-- This permanently deletes the listed data. Review the table list before running.
-- Staff accounts, students, student authentication, roles, and reusable venues
-- are intentionally preserved. Run the new catalogue seed after this file.
BEGIN;

DO $$
DECLARE
    table_name text;
    tables_to_reset constant text[] := ARRAY[
        'attendance',
        'incident',
        'generated_report',
        'invigilator_assignment',
        'student_venue_allocation',
        'exam_venue',
        'examination_slip',
        'examination_pass',
        'exam_session_programme_course',
        'exam_session',
        'student_registration',
        'student_programme_enrolment',
        'course_lecturer',
        'programme_course',
        'major',
        'programme',
        'school',
        'course'
    ];
BEGIN
    FOREACH table_name IN ARRAY tables_to_reset LOOP
        IF to_regclass('public.' || table_name) IS NOT NULL THEN
            EXECUTE format(
                'TRUNCATE TABLE public.%I RESTART IDENTITY CASCADE',
                table_name
            );
        END IF;
    END LOOP;
END $$;

COMMIT;

-- Verification: all reset tables should return zero rows.
SELECT table_name,
       to_regclass('public.' || table_name) AS existing_table
FROM unnest(ARRAY[
    'attendance',
    'incident',
    'generated_report',
    'invigilator_assignment',
    'student_venue_allocation',
    'exam_venue',
    'examination_slip',
    'examination_pass',
    'exam_session_programme_course',
    'exam_session',
    'student_registration',
    'student_programme_enrolment',
    'course_lecturer',
    'programme_course',
    'major',
    'programme',
    'school',
    'course'
]) AS t(table_name);

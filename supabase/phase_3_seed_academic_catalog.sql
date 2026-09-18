-- Phase 3: Academic catalog seed data for manual execution in the Supabase SQL Editor.
--
-- This script assumes the following tables already exist:
-- public.school, public.programme, public.course, public.programme_course.
--
-- Schools and programme names are based on public UNZA programme information.
-- Course codes, course names, credit hours, and curriculum placement below are
-- DEMO DATA. Replace them with Registry-approved curriculum data before production.
--
-- The script is safe to run more than once. It does not delete any data.

BEGIN;

INSERT INTO public.school (school_code, school_name)
VALUES
    ('SNS', 'School of Natural Sciences'),
    ('ENG', 'School of Engineering'),
    ('EDU', 'School of Education')
ON CONFLICT (school_code) DO UPDATE
SET school_name = EXCLUDED.school_name,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.programme (school_id, programme_code, programme_name, duration_years)
SELECT s.school_id, p.programme_code, p.programme_name, p.duration_years
FROM (
    VALUES
        ('SNS', 'BCS-CS',  'Bachelor of Computer Science in Computer Science', 5),
        ('SNS', 'BCS-SE',  'Bachelor of Computer Science in Software Engineering', 5),
        ('SNS', 'BCS-NIS', 'Bachelor of Computer Science in Networking and Information Security', 5),
        ('ENG', 'BENG-EE', 'Bachelor of Engineering in Electrical and Electronic Engineering', 5),
        ('ENG', 'BENG-CE', 'Bachelor of Engineering in Civil and Environmental Engineering', 5),
        ('ENG', 'BENG-ME', 'Bachelor of Engineering in Mechanical Engineering', 5),
        ('EDU', 'BED-MSE', 'Bachelor of Education - Secondary (Mathematics and Science)', 4),
        ('EDU', 'BED-PRI', 'Bachelor of Education (Primary Education)', 4),
        ('EDU', 'BSC-ED',  'Bachelor of Science with Education', 4)
) AS p(school_code, programme_code, programme_name, duration_years)
JOIN public.school s ON s.school_code = p.school_code
ON CONFLICT (programme_code) DO UPDATE
SET school_id = EXCLUDED.school_id,
    programme_name = EXCLUDED.programme_name,
    duration_years = EXCLUDED.duration_years,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.course (course_code, course_name, credit_hours)
VALUES
    ('CSC1101', 'Foundations of Computing', 3),
    ('CSC1202', 'Programming Fundamentals', 3),
    ('CSC2201', 'Data Structures and Algorithms', 3),
    ('CSC2302', 'Database Systems', 3),
    ('CSC3101', 'Software Engineering Principles', 3),
    ('CSC3202', 'Computer Networks', 3),
    ('CSC3303', 'Information Security Fundamentals', 3),
    ('CSC4101', 'Final Year Computing Project', 6),
    ('ENG1101', 'Engineering Mathematics I', 3),
    ('ENG1202', 'Engineering Drawing and Design', 3),
    ('EEE2101', 'Circuit Theory', 3),
    ('EEE2202', 'Digital Electronics', 3),
    ('CEE2101', 'Engineering Mechanics', 3),
    ('CEE2202', 'Fluid Mechanics', 3),
    ('MEE2101', 'Thermodynamics', 3),
    ('MEE2202', 'Manufacturing Processes', 3),
    ('EDU1101', 'Foundations of Education', 3),
    ('EDU1202', 'Educational Psychology', 3),
    ('EDU2101', 'Curriculum Studies', 3),
    ('EDU2202', 'Assessment and Evaluation', 3),
    ('MAT1101', 'Calculus for Science and Education', 3),
    ('MAT2101', 'Applied Statistics', 3)
ON CONFLICT (course_code) DO UPDATE
SET course_name = EXCLUDED.course_name,
    credit_hours = EXCLUDED.credit_hours,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.programme_course (
    programme_id,
    course_code,
    year_of_study,
    semester,
    course_category
)
SELECT p.programme_id, m.course_code, m.year_of_study, m.semester, m.course_category
FROM (
    VALUES
        ('BCS-CS',  'CSC1101', 1, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC1202', 1, 1, 'COMPULSORY'),
        ('BCS-CS',  'MAT1101', 1, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC2201', 2, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC2302', 2, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC3101', 3, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC3202', 3, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC3303', 4, 1, 'COMPULSORY'),
        ('BCS-CS',  'CSC4101', 5, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC1101', 1, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC1202', 1, 1, 'COMPULSORY'),
        ('BCS-SE',  'MAT1101', 1, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC2201', 2, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC2302', 2, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC3101', 3, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC3202', 3, 1, 'COMPULSORY'),
        ('BCS-SE',  'CSC3303', 4, 1, 'ELECTIVE'),
        ('BCS-NIS', 'CSC1101', 1, 1, 'COMPULSORY'),
        ('BCS-NIS', 'CSC1202', 1, 1, 'COMPULSORY'),
        ('BCS-NIS', 'MAT1101', 1, 1, 'COMPULSORY'),
        ('BCS-NIS', 'CSC2201', 2, 1, 'COMPULSORY'),
        ('BCS-NIS', 'CSC2302', 2, 1, 'COMPULSORY'),
        ('BCS-NIS', 'CSC3202', 3, 1, 'COMPULSORY'),
        ('BCS-NIS', 'CSC3303', 3, 1, 'COMPULSORY'),
        ('BENG-EE', 'ENG1101', 1, 1, 'COMPULSORY'),
        ('BENG-EE', 'ENG1202', 1, 1, 'COMPULSORY'),
        ('BENG-EE', 'EEE2101', 2, 1, 'COMPULSORY'),
        ('BENG-EE', 'EEE2202', 2, 1, 'COMPULSORY'),
        ('BENG-CE', 'ENG1101', 1, 1, 'COMPULSORY'),
        ('BENG-CE', 'ENG1202', 1, 1, 'COMPULSORY'),
        ('BENG-CE', 'CEE2101', 2, 1, 'COMPULSORY'),
        ('BENG-CE', 'CEE2202', 2, 1, 'COMPULSORY'),
        ('BENG-ME', 'ENG1101', 1, 1, 'COMPULSORY'),
        ('BENG-ME', 'ENG1202', 1, 1, 'COMPULSORY'),
        ('BENG-ME', 'MEE2101', 2, 1, 'COMPULSORY'),
        ('BENG-ME', 'MEE2202', 2, 1, 'COMPULSORY'),
        ('BED-MSE', 'EDU1101', 1, 1, 'COMPULSORY'),
        ('BED-MSE', 'EDU1202', 1, 1, 'COMPULSORY'),
        ('BED-MSE', 'MAT1101', 1, 1, 'COMPULSORY'),
        ('BED-MSE', 'EDU2101', 2, 1, 'COMPULSORY'),
        ('BED-MSE', 'EDU2202', 2, 1, 'COMPULSORY'),
        ('BED-MSE', 'MAT2101', 2, 1, 'COMPULSORY'),
        ('BED-PRI', 'EDU1101', 1, 1, 'COMPULSORY'),
        ('BED-PRI', 'EDU1202', 1, 1, 'COMPULSORY'),
        ('BED-PRI', 'EDU2101', 2, 1, 'COMPULSORY'),
        ('BED-PRI', 'EDU2202', 2, 1, 'COMPULSORY'),
        ('BSC-ED',  'EDU1101', 1, 1, 'COMPULSORY'),
        ('BSC-ED',  'MAT1101', 1, 1, 'COMPULSORY'),
        ('BSC-ED',  'EDU1202', 2, 1, 'COMPULSORY'),
        ('BSC-ED',  'MAT2101', 2, 1, 'COMPULSORY')
) AS m(programme_code, course_code, year_of_study, semester, course_category)
JOIN public.programme p ON p.programme_code = m.programme_code
ON CONFLICT (programme_id, course_code, year_of_study, semester) DO UPDATE
SET course_category = EXCLUDED.course_category,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

-- Optional checks after execution:
-- SELECT * FROM public.school ORDER BY school_code;
-- SELECT p.programme_code, p.programme_name, s.school_name
-- FROM public.programme p JOIN public.school s ON s.school_id = p.school_id
-- ORDER BY s.school_code, p.programme_code;
-- SELECT p.programme_code, pc.year_of_study, pc.semester, c.course_code, c.course_name
-- FROM public.programme_course pc
-- JOIN public.programme p ON p.programme_id = pc.programme_id
-- JOIN public.course c ON c.course_code = pc.course_code
-- ORDER BY p.programme_code, pc.year_of_study, pc.semester, c.course_code;

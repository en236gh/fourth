-- Phase 18: replace the academic catalogue definitions with the confirmed
-- three-school catalogue.
-- Run after V30__programme_majors.sql. PostgreSQL 15+.
-- Codes are generated from the confirmed names because source codes were N/A.
-- This script does not invent programme-course mappings. Existing curriculum
-- rows and exam links are retained until approved mappings are supplied.
BEGIN;

INSERT INTO public.school (school_code, school_name, is_active, updated_at)
VALUES
    ('SNS', 'School of Natural Sciences', TRUE, CURRENT_TIMESTAMP),
    ('ENG', 'School of Engineering', TRUE, CURRENT_TIMESTAMP),
    ('EDU', 'School of Education', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (school_code) DO UPDATE SET
    school_name = EXCLUDED.school_name,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.course (course_code, course_name, credit_hours, is_active, updated_at)
VALUES
    ('CEE2101', 'Engineering Mechanics', 3, TRUE, CURRENT_TIMESTAMP),
    ('CEE2202', 'Fluid Mechanics', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC1101', 'Foundations of Computing', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC1202', 'Programming Fundamentals', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC2201', 'Data Structures and Algorithms', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC2302', 'Database Systems', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC3101', 'Software Engineering Principles', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC3202', 'Computer Networks', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC3303', 'Information Security Fundamentals', 3, TRUE, CURRENT_TIMESTAMP),
    ('CSC4101', 'Final Year Computing Project', 6, TRUE, CURRENT_TIMESTAMP),
    ('EDU1101', 'Foundations of Education', 3, TRUE, CURRENT_TIMESTAMP),
    ('EDU1202', 'Educational Psychology', 3, TRUE, CURRENT_TIMESTAMP),
    ('EDU2101', 'Curriculum Studies', 3, TRUE, CURRENT_TIMESTAMP),
    ('EDU2202', 'Assessment and Evaluation', 3, TRUE, CURRENT_TIMESTAMP),
    ('EEE2101', 'Circuit Theory', 3, TRUE, CURRENT_TIMESTAMP),
    ('EEE2202', 'Digital Electronics', 3, TRUE, CURRENT_TIMESTAMP),
    ('ENG1101', 'Engineering Mathematics I', 3, TRUE, CURRENT_TIMESTAMP),
    ('ENG1202', 'Engineering Drawing and Design', 3, TRUE, CURRENT_TIMESTAMP),
    ('MAT1101', 'Calculus for Science and Education', 3, TRUE, CURRENT_TIMESTAMP),
    ('MAT2101', 'Applied Statistics', 3, TRUE, CURRENT_TIMESTAMP),
    ('MEE2101', 'Thermodynamics', 3, TRUE, CURRENT_TIMESTAMP),
    ('MEE2202', 'Manufacturing Processes', 3, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (course_code) DO UPDATE SET
    course_name = EXCLUDED.course_name,
    credit_hours = EXCLUDED.credit_hours,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.programme (school_id, programme_code, programme_name, duration_years, is_active, updated_at)
SELECT s.school_id, p.programme_code, p.programme_name, p.duration_years, TRUE, CURRENT_TIMESTAMP
FROM (VALUES
('SNS', 'SNS-BCS', 'Bachelor of Science in Computer Science', 4),
('SNS', 'SNS-BSC', 'Bachelor of Science', 4),
('SNS', 'SNS-BSC-BIO', 'Bachelor of Science (Biological Sciences Pathway)', 4),
('SNS', 'SNS-BSC-CPS', 'Bachelor of Science (Chemical & Physical Sciences Pathway)', 4),
('SNS', 'SNS-BSC-JM', 'Bachelor of Science (Joint Majors)', 4),
('ENG', 'ENG-BENG-AG', 'Bachelor of Engineering (Agricultural Engineering)', 5),
('ENG', 'ENG-BENG-CEE', 'Bachelor of Engineering (Civil and Environmental Engineering)', 5),
('ENG', 'ENG-BENG-EEE', 'Bachelor of Engineering (Electrical and Electronic Engineering)', 5),
('ENG', 'ENG-BENG-GEO', 'Bachelor of Engineering (Geomatic Engineering)', 5),
('ENG', 'ENG-BENG-ME', 'Bachelor of Engineering (Mechanical Engineering)', 5),
('EDU', 'EDU-BAE', 'Bachelor of Arts with Education', 4),
('EDU', 'EDU-BSC-ED', 'Bachelor of Science with Education', 4),
('EDU', 'EDU-BED-ENV', 'Bachelor of Education (Environmental Education)', 4),
('EDU', 'EDU-BSC-ICT-ED', 'Bachelor of Science in Information and Communication Technologies with Education', 4),
('EDU', 'EDU-BLIS', 'Bachelor of Library and Information Science', 4),
('EDU', 'EDU-BED-SPED', 'Bachelor of Education (Special Education)', 4),
('EDU', 'EDU-BED-EAM', 'Bachelor of Education (Educational Administration and Management)', 4),
('EDU', 'EDU-BED-PRI', 'Bachelor of Education (Primary Education)', 4),
('EDU', 'EDU-BED-ECE', 'Bachelor of Education (Early Childhood Education)', 4),
('EDU', 'EDU-BAE-ADULT', 'Bachelor of Adult Education', 4),
('EDU', 'EDU-BED-GC', 'Bachelor of Education (Guidance and Counselling)', 4)
) AS p(school_code, programme_code, programme_name, duration_years)
JOIN public.school s ON s.school_code = p.school_code
ON CONFLICT (programme_code) DO UPDATE SET
    school_id = EXCLUDED.school_id,
    programme_name = EXCLUDED.programme_name,
    duration_years = EXCLUDED.duration_years,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO public.major (programme_id, major_code, major_name, is_active)
SELECT p.programme_id, m.major_code, m.major_name, TRUE
FROM (VALUES
('SNS-BCS', 'SNS-CS', 'Computer Science'),
('SNS-BCS', 'SNS-SE', 'Software Engineering'),
('SNS-BCS', 'SNS-CSE', 'Computer Systems Engineering'),
('SNS-BCS', 'SNS-CNIS', 'Computer Networks and Information Security'),
('SNS-BSC', 'SNS-ACT', 'Actuarial Science'),
('SNS-BSC', 'SNS-MAT', 'Mathematics'),
('SNS-BSC', 'SNS-STAT', 'Statistics'),
('SNS-BSC-BIO', 'SNS-BIO', 'Biological Sciences'),
('SNS-BSC-BIO', 'SNS-MIC', 'Microbiology'),
('SNS-BSC-BIO', 'SNS-MBG', 'Molecular Biology and Genetics'),
('SNS-BSC-BIO', 'SNS-PAR', 'Parasitology'),
('SNS-BSC-BIO', 'SNS-EWM', 'Ecology and Wildlife Management'),
('SNS-BSC-CPS', 'SNS-CHEM', 'Chemistry'),
('SNS-BSC-CPS', 'SNS-PHY', 'Physics'),
('SNS-BSC-CPS', 'SNS-ENRM', 'Environmental and Natural Resource Management'),
('SNS-BSC-JM', 'SNS-CABS', 'Chemical and Biological Sciences'),
('SNS-BSC-JM', 'SNS-CB', 'Chemistry and Biology'),
('SNS-BSC-JM', 'SNS-CG', 'Chemistry and Geology'),
('SNS-BSC-JM', 'SNS-PM', 'Physics and Mathematics'),
('SNS-BSC-JM', 'SNS-PG', 'Physics and Geology'),
('ENG-BENG-AG', 'ENG-AG', 'Agricultural Engineering'),
('ENG-BENG-CEE', 'ENG-CEE', 'Civil and Environmental Engineering'),
('ENG-BENG-EEE', 'ENG-EMP', 'Electrical Machines and Power Engineering (EMP)'),
('ENG-BENG-EEE', 'ENG-ET', 'Electronic and Telecommunications Engineering (ET)'),
('ENG-BENG-GEO', 'ENG-GEO', 'Geomatic Engineering'),
('ENG-BENG-ME', 'ENG-ME', 'Mechanical Engineering'),
('EDU-BAE', 'EDU-HIST', 'History'),
('EDU-BAE', 'EDU-GEO', 'Geography'),
('EDU-BAE', 'EDU-ENG', 'English'),
('EDU-BAE', 'EDU-ZL', 'Zambian Languages'),
('EDU-BAE', 'EDU-CE', 'Civic Education'),
('EDU-BAE', 'EDU-RELS', 'Religious Studies'),
('EDU-BSC-ED', 'EDU-MAT', 'Mathematics'),
('EDU-BSC-ED', 'EDU-BIO', 'Biology'),
('EDU-BSC-ED', 'EDU-CHEM', 'Chemistry'),
('EDU-BSC-ED', 'EDU-PHY', 'Physics'),
('EDU-BED-ENV', 'EDU-ENV', 'Environmental Education'),
('EDU-BSC-ICT-ED', 'EDU-ICT', 'ICT and Education'),
('EDU-BLIS', 'EDU-LIS', 'Library and Information Studies'),
('EDU-BED-SPED', 'EDU-VI', 'Visual Impairment'),
('EDU-BED-SPED', 'EDU-HI', 'Hearing Impairment'),
('EDU-BED-SPED', 'EDU-LD', 'Learning Disabilities'),
('EDU-BED-EAM', 'EDU-EM', 'Educational Management'),
('EDU-BED-PRI', 'EDU-PRI', 'Primary Education Teachings'),
('EDU-BED-ECE', 'EDU-ECD', 'Early Childhood Development'),
('EDU-BAE-ADULT', 'EDU-AL', 'Adult Literacy'),
('EDU-BAE-ADULT', 'EDU-CD', 'Community Development'),
('EDU-BED-GC', 'EDU-EGC', 'Educational Guidance and Counselling')
) AS m(programme_code, major_code, major_name)
JOIN public.programme p ON p.programme_code = m.programme_code
ON CONFLICT (major_code) DO UPDATE SET
    programme_id = EXCLUDED.programme_id,
    major_name = EXCLUDED.major_name,
    is_active = TRUE;

UPDATE public.major SET is_active = FALSE;
UPDATE public.major
SET is_active = TRUE
WHERE major_code IN (
    'SNS-CS', 'SNS-SE', 'SNS-CSE', 'SNS-CNIS', 'SNS-ACT', 'SNS-MAT', 'SNS-STAT',
    'SNS-BIO', 'SNS-MIC', 'SNS-MBG', 'SNS-PAR', 'SNS-EWM', 'SNS-CHEM', 'SNS-PHY',
    'SNS-ENRM', 'SNS-CABS', 'SNS-CB', 'SNS-CG', 'SNS-PM', 'SNS-PG', 'ENG-AG',
    'ENG-CEE', 'ENG-EMP', 'ENG-ET', 'ENG-GEO', 'ENG-ME', 'EDU-HIST', 'EDU-GEO',
    'EDU-ENG', 'EDU-ZL', 'EDU-CE', 'EDU-RELS', 'EDU-MAT', 'EDU-BIO', 'EDU-CHEM',
    'EDU-PHY', 'EDU-ENV', 'EDU-ICT', 'EDU-LIS', 'EDU-VI', 'EDU-HI', 'EDU-LD',
    'EDU-EM', 'EDU-PRI', 'EDU-ECD', 'EDU-AL', 'EDU-CD', 'EDU-EGC'
);

UPDATE public.programme SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP;
UPDATE public.programme
SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP
WHERE programme_code IN (
    'SNS-BCS', 'SNS-BSC', 'SNS-BSC-BIO', 'SNS-BSC-CPS', 'SNS-BSC-JM',
    'ENG-BENG-AG', 'ENG-BENG-CEE', 'ENG-BENG-EEE', 'ENG-BENG-GEO', 'ENG-BENG-ME',
    'EDU-BAE', 'EDU-BSC-ED', 'EDU-BED-ENV', 'EDU-BSC-ICT-ED', 'EDU-BLIS',
    'EDU-BED-SPED', 'EDU-BED-EAM', 'EDU-BED-PRI', 'EDU-BED-ECE', 'EDU-BAE-ADULT',
    'EDU-BED-GC'
);

COMMIT;

SELECT s.school_code, p.programme_code, p.programme_name,
       m.major_code, m.major_name, p.is_active, m.is_active
FROM public.programme p
JOIN public.school s ON s.school_id = p.school_id
LEFT JOIN public.major m ON m.programme_id = p.programme_id
WHERE p.is_active
ORDER BY s.school_code, p.programme_code, m.major_code;
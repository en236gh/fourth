-- Phase 23: add demo lecturers and assign the current course catalogue.
-- Run after V27/V28, V30, and phase_18_replace_academic_catalog.sql.
-- Shared password for the new accounts: e1n2o3c4h5 (BCrypt hash).
-- Existing lecturer accounts and course_lecturer links are retained.
BEGIN;

INSERT INTO public.role (name)
VALUES ('LECTURER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.staff (
    full_name,
    email,
    phone,
    department,
    account_status,
    password_hash
)
VALUES
    ('Lecturer Six', 'lecturer6@gmail.com', '+260970000009', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Seven', 'lecturer7@gmail.com', '+260970000010', 'Mathematics', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Eight', 'lecturer8@gmail.com', '+260970000011', 'Engineering', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Nine', 'lecturer9@gmail.com', '+260970000012', 'Electrical Engineering', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Ten', 'lecturer10@gmail.com', '+260970000013', 'Civil Engineering', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Eleven', 'lecturer11@gmail.com', '+260970000014', 'Mechanical Engineering', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Twelve', 'lecturer12@gmail.com', '+260970000015', 'Education', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Thirteen', 'lecturer13@gmail.com', '+260970000016', 'Education', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6')
ON CONFLICT (email) DO UPDATE
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    department = EXCLUDED.department,
    account_status = EXCLUDED.account_status,
    password_hash = EXCLUDED.password_hash;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT st.staff_id, r.role_id
FROM public.staff st
JOIN public.role r ON r.name = 'LECTURER'
WHERE st.email IN (
    'lecturer6@gmail.com', 'lecturer7@gmail.com', 'lecturer8@gmail.com',
    'lecturer9@gmail.com', 'lecturer10@gmail.com', 'lecturer11@gmail.com',
    'lecturer12@gmail.com', 'lecturer13@gmail.com'
)
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    assignment RECORD;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (VALUES
            ('lecturer@gmail.com'),
            ('lecturer2@gmail.com'),
            ('lecturer3@gmail.com'),
            ('lecturer4@gmail.com'),
            ('lecturer5@gmail.com')
        ) AS required(email)
        WHERE NOT EXISTS (
            SELECT 1
            FROM public.staff st
            JOIN public.staff_role sr ON sr.staff_id = st.staff_id
            JOIN public.role r ON r.role_id = sr.role_id
            WHERE st.email = required.email
              AND st.account_status = 'ACTIVE'
              AND r.name = 'LECTURER'
        )
    ) THEN
        RAISE EXCEPTION 'Run the existing lecturer account seed first; one or more of lecturer@gmail.com through lecturer5@gmail.com are missing or inactive';
    END IF;

    FOR assignment IN
        SELECT * FROM (VALUES
            ('lecturer@gmail.com', 'CSC1101'),
            ('lecturer2@gmail.com', 'CSC1202'),
            ('lecturer3@gmail.com', 'CSC2201'),
            ('lecturer4@gmail.com', 'CSC2302'),
            ('lecturer5@gmail.com', 'CSC3101'),
            ('lecturer6@gmail.com', 'CSC3202'),
            ('lecturer6@gmail.com', 'CSC3303'),
            ('lecturer6@gmail.com', 'CSC4101'),
            ('lecturer7@gmail.com', 'MAT1101'),
            ('lecturer7@gmail.com', 'MAT2101'),
            ('lecturer8@gmail.com', 'ENG1101'),
            ('lecturer8@gmail.com', 'ENG1202'),
            ('lecturer9@gmail.com', 'EEE2101'),
            ('lecturer9@gmail.com', 'EEE2202'),
            ('lecturer10@gmail.com', 'CEE2101'),
            ('lecturer10@gmail.com', 'CEE2202'),
            ('lecturer11@gmail.com', 'MEE2101'),
            ('lecturer11@gmail.com', 'MEE2202'),
            ('lecturer12@gmail.com', 'EDU1101'),
            ('lecturer12@gmail.com', 'EDU1202'),
            ('lecturer13@gmail.com', 'EDU2101'),
            ('lecturer13@gmail.com', 'EDU2202')
        ) AS assignments(email, course_code)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM public.staff st
            JOIN public.staff_role sr ON sr.staff_id = st.staff_id
            JOIN public.role r ON r.role_id = sr.role_id
            WHERE st.email = assignment.email
              AND st.account_status = 'ACTIVE'
              AND r.name = 'LECTURER'
        ) THEN
            RAISE EXCEPTION 'Missing active lecturer account/role: %', assignment.email;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM public.course c
            WHERE c.course_code = assignment.course_code
              AND c.is_active
        ) THEN
            RAISE EXCEPTION 'Missing active course in the academic catalogue: %', assignment.course_code;
        END IF;

        INSERT INTO public.course_lecturer (course_code, staff_id)
        SELECT assignment.course_code, st.staff_id
        FROM public.staff st
        WHERE st.email = assignment.email
        ON CONFLICT (course_code, staff_id) DO NOTHING;
    END LOOP;
END $$;

COMMIT;

-- Verify lecturer accounts and their course assignments.
SELECT st.email,
       st.full_name,
       st.department,
       COUNT(DISTINCT cl.course_code) AS assigned_courses,
       STRING_AGG(DISTINCT cl.course_code, ', ' ORDER BY cl.course_code) AS courses
FROM public.staff st
JOIN public.staff_role sr ON sr.staff_id = st.staff_id
JOIN public.role r ON r.role_id = sr.role_id AND r.name = 'LECTURER'
LEFT JOIN public.course_lecturer cl ON cl.staff_id = st.staff_id
WHERE st.email IN (
    'lecturer@gmail.com', 'lecturer2@gmail.com', 'lecturer3@gmail.com',
    'lecturer4@gmail.com', 'lecturer5@gmail.com', 'lecturer6@gmail.com',
    'lecturer7@gmail.com', 'lecturer8@gmail.com', 'lecturer9@gmail.com',
    'lecturer10@gmail.com', 'lecturer11@gmail.com', 'lecturer12@gmail.com',
    'lecturer13@gmail.com'
)
GROUP BY st.staff_id, st.email, st.full_name, st.department
ORDER BY st.email;
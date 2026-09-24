-- Add four demo lecturers using the existing lecturer account's BCrypt hash.
-- Shared demo password: e1n2o3c4h5

INSERT INTO public.role (name)
VALUES ('LECTURER')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.staff (full_name, email, phone, department, account_status, password_hash)
VALUES
    ('Lecturer Two', 'lecturer2@gmail.com', '+260970000005', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Three', 'lecturer3@gmail.com', '+260970000006', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Four', 'lecturer4@gmail.com', '+260970000007', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Lecturer Five', 'lecturer5@gmail.com', '+260970000008', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6')
ON CONFLICT (email) DO UPDATE
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    department = EXCLUDED.department,
    account_status = EXCLUDED.account_status,
    password_hash = EXCLUDED.password_hash;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM public.staff s
JOIN public.role r ON r.name = 'LECTURER'
WHERE s.email IN (
    'lecturer2@gmail.com',
    'lecturer3@gmail.com',
    'lecturer4@gmail.com',
    'lecturer5@gmail.com'
)
ON CONFLICT DO NOTHING;

-- Seed the shared demo staff accounts with BCrypt password hashes.

INSERT INTO public.role (name)
VALUES ('ADMINISTRATOR'), ('LECTURER'), ('INVIGILATOR')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.staff (full_name, email, phone, department, account_status, password_hash)
VALUES
    ('Enoch Simfukwe', 'simfukweenoch@gmail.com', '+260970000001', 'Examinations Office', 'ACTIVE', '$2a$12$mlF0QQgoASzyl7H/TZHxuuWB4wi7oc82nhnNfVQGGcA0IcPYRwaRa'),
    ('Lecturer Account', 'lecturer@gmail.com', '+260970000002', 'Computer Science', 'ACTIVE', '$2a$12$N51LBcHbrcjfUXxzAloTEeBBGOKRwxR/NNEb6bT5FTGiBR/WAZHj6'),
    ('Invigilator One', 'invigilator1@gmail.com', '+260970000003', 'Examinations Office', 'ACTIVE', '$2a$12$BdndVrlsFLowvI7ITmd40e/pvMqGRMGNP3WuqFISJKuiVqiAI8tQG'),
    ('Invigilator Two', 'invigilator2@gmail.com', '+260970000004', 'Examinations Office', 'ACTIVE', '$2a$12$ZtB3.AFXRf84sRfjDWQ/bOrayQcQYhewO9FTV7Z2BGqw6WJg8MtUa')
ON CONFLICT (email) DO UPDATE
SET full_name = EXCLUDED.full_name,
    phone = EXCLUDED.phone,
    department = EXCLUDED.department,
    account_status = EXCLUDED.account_status,
    password_hash = EXCLUDED.password_hash;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM public.staff s
JOIN public.role r ON r.name = CASE s.email
    WHEN 'simfukweenoch@gmail.com' THEN 'ADMINISTRATOR'
    WHEN 'lecturer@gmail.com' THEN 'LECTURER'
    ELSE 'INVIGILATOR'
END
WHERE s.email IN (
    'simfukweenoch@gmail.com',
    'lecturer@gmail.com',
    'invigilator1@gmail.com',
    'invigilator2@gmail.com'
)
ON CONFLICT DO NOTHING;
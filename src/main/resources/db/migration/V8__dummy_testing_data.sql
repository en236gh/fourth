-- Dummy test data for end-to-end examination attendance testing.
-- This seed set mirrors the core entities used by the attendance flow:
-- staff, roles, students, venues, exam sessions, and student allocations.

INSERT INTO public.role (name)
VALUES ('ADMINISTRATOR'), ('LECTURER'), ('INVIGILATOR')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.staff (staff_no, full_name, email, phone, department, password_hash)
VALUES
    ('STF001', 'A. Banda', 'admin@unza.zm', '+260971000001', 'Registry', '$2b$12$pvimFufr708hBc8WOuKnaeyO46RES//Vck2ldWHALHIY05Vt/JuCC'),
    ('STF002', 'T. Mwewa', 'invigilator@unza.zm', '+260971000002', 'Examinations', '$2b$12$76FjPNsKOo3AJo4xppjLhei1fYsUIPU4tbdgV/lf4rrcwlodkY71u'),
    ('STF003', 'L. Phiri', 'invigilator2@unza.zm', '+260971000003', 'Examinations', '$2b$12$02v81RlPeHjujKgQb4MygO8LRAkdwqODbiyZFtJ4FxQrCoHlpn3uO'),
    ('STF004', 'K. Tembo', 'lecturer@unza.zm', '+260971000004', 'Computer Science', '$2b$12$SzWLZeydVBp2wE2D0rySpe.Lt8uFmLXQ9f3.uIIGLrwSSH8jTYYdy')
ON CONFLICT (email) DO NOTHING;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM public.staff s
JOIN public.role r ON r.name = 'ADMINISTRATOR'
WHERE s.email = 'admin@unza.zm'
ON CONFLICT DO NOTHING;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM public.staff s
JOIN public.role r ON r.name = 'INVIGILATOR'
WHERE s.email IN ('invigilator@unza.zm', 'invigilator2@unza.zm')
ON CONFLICT DO NOTHING;

INSERT INTO public.staff_role (staff_id, role_id)
SELECT s.staff_id, r.role_id
FROM public.staff s
JOIN public.role r ON r.name = 'LECTURER'
WHERE s.email = 'lecturer@unza.zm'
ON CONFLICT DO NOTHING;

INSERT INTO public.student (computer_number, national_id, full_name, program, year_of_study, email, phone, photo_path, qr_token, status)
VALUES
    ('2022004264', '1234567890', 'K. Banda', 'Computer Science', 3, 'k.banda@dummy.unza', '+260971100001', '/img/students/kbanda.jpg', 'QR-CS-001', 'ACTIVE'),
    ('2022004265', '1234567891', 'T. Mwewa', 'Computer Science', 3, 't.mwewa@dummy.unza', '+260971100002', '/img/students/tmwewa.jpg', 'QR-CS-002', 'ACTIVE'),
    ('2022004266', '1234567892', 'L. Phiri', 'Computer Science', 3, 'l.phiri@dummy.unza', '+260971100003', '/img/students/lphiri.jpg', 'QR-CS-003', 'ACTIVE'),
    ('2022004267', '1234567893', 'J. Sialumba', 'Electrical Engineering', 4, 'j.sialumba@dummy.unza', '+260971100004', '/img/students/jsialumba.jpg', 'QR-EE-001', 'ACTIVE'),
    ('2022004268', '1234567894', 'M. Mulenga', 'Electrical Engineering', 4, 'm.mulenga@dummy.unza', '+260971100005', '/img/students/mmulenga.jpg', 'QR-EE-002', 'ACTIVE'),
    ('2022004269', '1234567895', 'N. Chanda', 'Education', 2, 'n.chanda@dummy.unza', '+260971100006', '/img/students/nchanda.jpg', 'QR-ED-001', 'ACTIVE'),
    ('2022004270', '1234567896', 'P. Mwaba', 'Education', 2, 'p.mwaba@dummy.unza', '+260971100007', '/img/students/pmwaba.jpg', 'QR-ED-002', 'ACTIVE'),
    ('2022004271', '1234567897', 'S. Kalima', 'Business Administration', 3, 's.kalima@dummy.unza', '+260971100008', '/img/students/skalima.jpg', 'QR-BA-001', 'ACTIVE')
ON CONFLICT (computer_number) DO NOTHING;

INSERT INTO public.venue (venue_name, building, capacity)
SELECT 'Main LT 1', 'Great East Road Campus', 250
WHERE NOT EXISTS (SELECT 1 FROM public.venue WHERE venue_name = 'Main LT 1');

INSERT INTO public.venue (venue_name, building, capacity)
SELECT 'Engineering Lab 2', 'School of Engineering', 120
WHERE NOT EXISTS (SELECT 1 FROM public.venue WHERE venue_name = 'Engineering Lab 2');

INSERT INTO public.venue (venue_name, building, capacity)
SELECT 'Education Block A', 'School of Education', 180
WHERE NOT EXISTS (SELECT 1 FROM public.venue WHERE venue_name = 'Education Block A');

INSERT INTO public.venue (venue_name, building, capacity)
SELECT 'Natural Science Hall', 'School of Natural Sciences', 200
WHERE NOT EXISTS (SELECT 1 FROM public.venue WHERE venue_name = 'Natural Science Hall');

INSERT INTO public.venue (venue_name, building, capacity)
SELECT 'Business Hall', 'Graduate School of Business', 150
WHERE NOT EXISTS (SELECT 1 FROM public.venue WHERE venue_name = 'Business Hall');

INSERT INTO public.exam_session (course_code, exam_date, start_time, end_time, academic_year, semester, exam_type)
SELECT 'CS101', '2026-08-10', '09:00:00', '11:00:00', '2025/2026', 1, 'FINAL'
WHERE NOT EXISTS (SELECT 1 FROM public.exam_session WHERE course_code = 'CS101' AND exam_date = '2026-08-10');

INSERT INTO public.exam_session (course_code, exam_date, start_time, end_time, academic_year, semester, exam_type)
SELECT 'EE221', '2026-08-12', '13:00:00', '15:00:00', '2025/2026', 1, 'FINAL'
WHERE NOT EXISTS (SELECT 1 FROM public.exam_session WHERE course_code = 'EE221' AND exam_date = '2026-08-12');

INSERT INTO public.exam_session (course_code, exam_date, start_time, end_time, academic_year, semester, exam_type)
SELECT 'ED201', '2026-08-14', '10:00:00', '12:00:00', '2025/2026', 1, 'FINAL'
WHERE NOT EXISTS (SELECT 1 FROM public.exam_session WHERE course_code = 'ED201' AND exam_date = '2026-08-14');

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004264', es.exam_session_id, v.venue_id, 'A01'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
WHERE es.course_code = 'CS101'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004265', es.exam_session_id, v.venue_id, 'A02'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
WHERE es.course_code = 'CS101'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004267', es.exam_session_id, v.venue_id, 'B01'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
WHERE es.course_code = 'EE221'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004268', es.exam_session_id, v.venue_id, 'B02'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
WHERE es.course_code = 'EE221'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004269', es.exam_session_id, v.venue_id, 'C01'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Education Block A'
WHERE es.course_code = 'ED201'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004270', es.exam_session_id, v.venue_id, 'C02'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Education Block A'
WHERE es.course_code = 'ED201'
ON CONFLICT DO NOTHING;

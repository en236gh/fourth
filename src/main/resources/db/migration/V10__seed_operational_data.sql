-- Seed read-only operational links: registrations, exam venues, invigilator assignments.
-- These represent data that already exists outside this attendance-focused application.

-- Registrations for CS101
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'CS101', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN ('2022004264', '2022004265', '2022004266', '2022004271')
ON CONFLICT DO NOTHING;

-- Registrations for EE221
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'EE221', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN ('2022004267', '2022004268')
ON CONFLICT DO NOTHING;

-- Registrations for ED201
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'ED201', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN ('2022004269', '2022004270')
ON CONFLICT DO NOTHING;

-- Capacity demo venues for CS101: 200 + 150
INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name IN ('Natural Science Hall', 'Business Hall')
WHERE es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

-- Also keep Main LT available for CS101 (existing V8 allocations)
INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
WHERE es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
WHERE es.course_code = 'EE221' AND es.exam_date = '2026-08-12'
ON CONFLICT DO NOTHING;

INSERT INTO public.exam_venue (exam_session_id, venue_id)
SELECT es.exam_session_id, v.venue_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Education Block A'
WHERE es.course_code = 'ED201' AND es.exam_date = '2026-08-14'
ON CONFLICT DO NOTHING;

-- Invigilator 1: CS101 @ Natural Science Hall
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Natural Science Hall'
JOIN public.staff s ON s.email = 'invigilator@unza.zm'
WHERE es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

-- Invigilator 1: CS101 @ Main LT 1 (matches existing V8 allocations)
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Main LT 1'
JOIN public.staff s ON s.email = 'invigilator@unza.zm'
WHERE es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

-- Invigilator 2: CS101 @ Business Hall
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Business Hall'
JOIN public.staff s ON s.email = 'invigilator2@unza.zm'
WHERE es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
ON CONFLICT DO NOTHING;

-- Invigilator 2: EE221 @ Engineering Lab 2
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
JOIN public.staff s ON s.email = 'invigilator2@unza.zm'
WHERE es.course_code = 'EE221' AND es.exam_date = '2026-08-12'
ON CONFLICT DO NOTHING;

-- Invigilator 1: ED201 @ Education Block A
INSERT INTO public.invigilator_assignment (exam_session_id, venue_id, staff_id)
SELECT es.exam_session_id, v.venue_id, s.staff_id
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Education Block A'
JOIN public.staff s ON s.email = 'invigilator@unza.zm'
WHERE es.course_code = 'ED201' AND es.exam_date = '2026-08-14'
ON CONFLICT DO NOTHING;

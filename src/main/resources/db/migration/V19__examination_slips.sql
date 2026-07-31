-- Phase 2: examination slips with per-exam signed QR tokens.
-- Also expands registrations/allocations so more seeded students can generate slips.

CREATE TABLE IF NOT EXISTS public.examination_slip (
    slip_id BIGSERIAL PRIMARY KEY,
    computer_number VARCHAR(15) NOT NULL REFERENCES public.student(computer_number) ON DELETE CASCADE,
    exam_session_id INTEGER NOT NULL REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE,
    qr_token TEXT NOT NULL,
    qr_jti VARCHAR(64) NOT NULL UNIQUE,
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_examination_slip_student_exam UNIQUE (computer_number, exam_session_id)
);

CREATE INDEX IF NOT EXISTS idx_examination_slip_student
    ON public.examination_slip(computer_number);

CREATE INDEX IF NOT EXISTS idx_examination_slip_exam
    ON public.examination_slip(exam_session_id);

-- Extra CS101 registrations for Phase-1 seeded students
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'CS101', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN (
    '2022004272', '2022004273', '2022004280', '2022004281'
)
ON CONFLICT DO NOTHING;

-- Extra EE221 registrations
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'EE221', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN ('2022004274', '2022004275', '2022004282')
ON CONFLICT DO NOTHING;

-- Extra ED201 registrations
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
SELECT s.computer_number, 'ED201', '2025/2026', 1
FROM public.student s
WHERE s.computer_number IN ('2022004276', '2022004277', '2022004283')
ON CONFLICT DO NOTHING;

-- Ensure CS101 Main LT allocations for core demo students (including newly registered)
INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT vals.computer_number, es.exam_session_id, v.venue_id, vals.seat_number
FROM (VALUES
    ('2022004264', 'A01'),
    ('2022004265', 'A02'),
    ('2022004266', 'A03'),
    ('2022004271', 'A04'),
    ('2022004272', 'A05'),
    ('2022004273', 'A06'),
    ('2022004280', 'A07'),
    ('2022004281', 'A08')
) AS vals(computer_number, seat_number)
JOIN public.exam_session es ON es.course_code = 'CS101' AND es.exam_date = '2026-08-10'
JOIN public.venue v ON v.venue_name = 'Main LT 1'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT vals.computer_number, es.exam_session_id, v.venue_id, vals.seat_number
FROM (VALUES
    ('2022004267', 'B01'),
    ('2022004268', 'B02'),
    ('2022004274', 'B03'),
    ('2022004275', 'B04'),
    ('2022004282', 'B05')
) AS vals(computer_number, seat_number)
JOIN public.exam_session es ON es.course_code = 'EE221' AND es.exam_date = '2026-08-12'
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT vals.computer_number, es.exam_session_id, v.venue_id, vals.seat_number
FROM (VALUES
    ('2022004269', 'C01'),
    ('2022004270', 'C02'),
    ('2022004276', 'C03'),
    ('2022004277', 'C04'),
    ('2022004283', 'C05')
) AS vals(computer_number, seat_number)
JOIN public.exam_session es ON es.course_code = 'ED201' AND es.exam_date = '2026-08-14'
JOIN public.venue v ON v.venue_name = 'Education Block A'
ON CONFLICT DO NOTHING;

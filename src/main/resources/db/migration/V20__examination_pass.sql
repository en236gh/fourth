-- Replace per-exam examination slips with one pass (one QR) per student per exam period.

CREATE TABLE IF NOT EXISTS public.examination_pass (
    pass_id BIGSERIAL PRIMARY KEY,
    computer_number VARCHAR(15) NOT NULL REFERENCES public.student(computer_number) ON DELETE CASCADE,
    academic_year VARCHAR(20) NOT NULL,
    semester INTEGER NOT NULL,
    qr_token TEXT NOT NULL,
    qr_jti VARCHAR(64) NOT NULL UNIQUE,
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uq_examination_pass_student_period UNIQUE (computer_number, academic_year, semester)
);

CREATE INDEX IF NOT EXISTS idx_examination_pass_student
    ON public.examination_pass(computer_number);

DROP TABLE IF EXISTS public.examination_slip;

-- Demo: give 2022004265 three allocated exams so one pass covers multiple papers
INSERT INTO public.student_registration (computer_number, course_code, academic_year, semester)
VALUES
    ('2022004265', 'EE221', '2025/2026', 1),
    ('2022004265', 'ED201', '2025/2026', 1)
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004265', es.exam_session_id, v.venue_id, 'A09'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Engineering Lab 2'
WHERE es.course_code = 'EE221' AND es.exam_date = '2026-08-12'
ON CONFLICT DO NOTHING;

INSERT INTO public.student_venue_allocation (computer_number, exam_session_id, venue_id, seat_number)
SELECT '2022004265', es.exam_session_id, v.venue_id, 'C06'
FROM public.exam_session es
JOIN public.venue v ON v.venue_name = 'Education Block A'
WHERE es.course_code = 'ED201' AND es.exam_date = '2026-08-14'
ON CONFLICT DO NOTHING;

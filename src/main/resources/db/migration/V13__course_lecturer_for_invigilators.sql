-- Restore course-lecturer links so invigilators can see which lecturer owns an exam's course.
-- Assignments are assumed to already exist outside this app; this recreates the read model.

CREATE TABLE IF NOT EXISTS public.course_lecturer (
    course_code VARCHAR(15) NOT NULL,
    staff_id INTEGER NOT NULL,
    PRIMARY KEY (course_code, staff_id),
    CONSTRAINT fk_course_lecturer_staff
        FOREIGN KEY (staff_id) REFERENCES public.staff (staff_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_lecturer_course
    ON public.course_lecturer (course_code);

-- Seed: lecturer@unza.zm owns the demo courses used by exam sessions.
INSERT INTO public.course_lecturer (course_code, staff_id)
SELECT 'CS101', s.staff_id
FROM public.staff s
WHERE s.email = 'lecturer@unza.zm'
ON CONFLICT DO NOTHING;

INSERT INTO public.course_lecturer (course_code, staff_id)
SELECT 'EE221', s.staff_id
FROM public.staff s
WHERE s.email = 'lecturer@unza.zm'
ON CONFLICT DO NOTHING;

INSERT INTO public.course_lecturer (course_code, staff_id)
SELECT 'ED201', s.staff_id
FROM public.staff s
WHERE s.email = 'lecturer@unza.zm'
ON CONFLICT DO NOTHING;

-- Older CSC101 sessions (if present) also point at the same lecturer.
INSERT INTO public.course_lecturer (course_code, staff_id)
SELECT 'CSC101', s.staff_id
FROM public.staff s
WHERE s.email = 'lecturer@unza.zm'
ON CONFLICT DO NOTHING;

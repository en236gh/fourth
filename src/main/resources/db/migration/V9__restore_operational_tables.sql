-- Restore operational tables required by lecturer allocation and invigilator workflows.
-- Assignments/registrations are assumed to already exist in the broader system;
-- this migration recreates the tables for the attendance app to read.

CREATE TABLE IF NOT EXISTS public.student_registration (
    computer_number VARCHAR(15) NOT NULL,
    course_code VARCHAR(15) NOT NULL,
    academic_year VARCHAR(9) NOT NULL,
    semester SMALLINT NOT NULL,
    PRIMARY KEY (computer_number, course_code, academic_year, semester),
    CONSTRAINT student_registration_semester_check CHECK (semester IN (1, 2)),
    CONSTRAINT fk_student_registration_student
        FOREIGN KEY (computer_number) REFERENCES public.student (computer_number) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_registration_course
    ON public.student_registration (course_code, academic_year, semester);

CREATE TABLE IF NOT EXISTS public.exam_venue (
    exam_session_id INTEGER NOT NULL,
    venue_id INTEGER NOT NULL,
    PRIMARY KEY (exam_session_id, venue_id),
    CONSTRAINT fk_exam_venue_exam
        FOREIGN KEY (exam_session_id) REFERENCES public.exam_session (exam_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_venue_venue
        FOREIGN KEY (venue_id) REFERENCES public.venue (venue_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS public.invigilator_assignment (
    exam_session_id INTEGER NOT NULL,
    venue_id INTEGER NOT NULL,
    staff_id INTEGER NOT NULL,
    PRIMARY KEY (exam_session_id, venue_id, staff_id),
    CONSTRAINT fk_invigilator_assignment_exam_venue
        FOREIGN KEY (exam_session_id, venue_id) REFERENCES public.exam_venue (exam_session_id, venue_id) ON DELETE CASCADE,
    CONSTRAINT fk_invigilator_assignment_staff
        FOREIGN KEY (staff_id) REFERENCES public.staff (staff_id) ON DELETE RESTRICT
);

ALTER TABLE public.exam_session
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED';

UPDATE public.exam_session
SET status = 'SCHEDULED'
WHERE status IS NULL OR status = '';

ALTER TABLE public.exam_session
    DROP CONSTRAINT IF EXISTS exam_session_status_check;

ALTER TABLE public.exam_session
    ADD CONSTRAINT exam_session_status_check
    CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED'));

ALTER TABLE public.incident
    ADD COLUMN IF NOT EXISTS venue_id INTEGER;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'incident'
          AND column_name = 'incident_time'
    ) THEN
        ALTER TABLE public.incident ALTER COLUMN incident_time DROP NOT NULL;
    END IF;
END $$;

ALTER TABLE public.incident
    ALTER COLUMN description TYPE VARCHAR(1000);

ALTER TABLE public.incident
    DROP CONSTRAINT IF EXISTS incident_type_check;

ALTER TABLE public.incident
    ADD CONSTRAINT incident_type_check
    CHECK (incident_type IN (
        'CHEATING',
        'PHONE_FOUND',
        'WRONG_VENUE',
        'MEDICAL_EMERGENCY',
        'DISTURBANCE',
        'LATE_ARRIVAL',
        'OTHER'
    ));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_incident_venue'
    ) THEN
        ALTER TABLE public.incident
            ADD CONSTRAINT fk_incident_venue
            FOREIGN KEY (venue_id) REFERENCES public.venue (venue_id) ON DELETE SET NULL;
    END IF;
END $$;

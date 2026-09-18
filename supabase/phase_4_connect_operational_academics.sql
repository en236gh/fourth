-- Phase 4: Connect the existing exam-attendance operational schema to the
-- academic catalog created in Phase 2 and seeded in Phase 3.
--
-- Reference schema: exam_attendance.sql
-- Existing tables used: student, student_registration, course_lecturer,
-- exam_session, exam_venue, and student_venue_allocation.
--
-- Run manually in the Supabase SQL Editor after phase_3_seed_academic_catalog.sql.
-- This script does not delete or rename existing operational data.

BEGIN;

-- Stop safely if operational tables contain a course code not yet present in
-- public.course. Add that course to the Phase 3 catalog, then re-run this script.
DO $$
DECLARE
    missing_codes text;
BEGIN
    SELECT string_agg(course_code, ', ' ORDER BY course_code)
    INTO missing_codes
    FROM (
        SELECT DISTINCT es.course_code
        FROM public.exam_session es
        LEFT JOIN public.course c ON c.course_code = es.course_code
        WHERE c.course_code IS NULL
        UNION
        SELECT DISTINCT sr.course_code
        FROM public.student_registration sr
        LEFT JOIN public.course c ON c.course_code = sr.course_code
        WHERE c.course_code IS NULL
        UNION
        SELECT DISTINCT cl.course_code
        FROM public.course_lecturer cl
        LEFT JOIN public.course c ON c.course_code = cl.course_code
        WHERE c.course_code IS NULL
    ) missing;

    IF missing_codes IS NOT NULL THEN
        RAISE EXCEPTION
            'Phase 4 stopped. Add these course codes to public.course first: %',
            missing_codes;
    END IF;
END $$;

-- One student can have one programme enrolment for each academic year.
-- This preserves programme/year history instead of relying only on the current
-- text fields in public.student.
CREATE TABLE IF NOT EXISTS public.student_programme_enrolment (
    enrolment_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    computer_number character varying(15) NOT NULL,
    programme_id integer NOT NULL,
    academic_year character varying(9) NOT NULL,
    year_of_study smallint NOT NULL,
    enrolment_status character varying(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT student_programme_enrolment_year_check CHECK (year_of_study > 0),
    CONSTRAINT student_programme_enrolment_status_check
        CHECK (enrolment_status IN ('ACTIVE', 'DEFERRED', 'SUSPENDED', 'COMPLETED')),
    CONSTRAINT student_programme_enrolment_unique_student_year
        UNIQUE (computer_number, academic_year),
    CONSTRAINT fk_student_programme_enrolment_student
        FOREIGN KEY (computer_number) REFERENCES public.student(computer_number) ON DELETE CASCADE,
    CONSTRAINT fk_student_programme_enrolment_programme
        FOREIGN KEY (programme_id) REFERENCES public.programme(programme_id) ON DELETE RESTRICT
);

-- An exam may serve several programmes for the same course. This junction is
-- intentionally used instead of putting one programme_course_id on exam_session.
CREATE TABLE IF NOT EXISTS public.exam_session_programme_course (
    exam_session_id integer NOT NULL,
    programme_course_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (exam_session_id, programme_course_id),
    CONSTRAINT fk_exam_session_programme_course_exam_session
        FOREIGN KEY (exam_session_id) REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_session_programme_course_programme_course
        FOREIGN KEY (programme_course_id)
        REFERENCES public.programme_course(programme_course_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_student_programme_enrolment_programme_year
    ON public.student_programme_enrolment (programme_id, academic_year, year_of_study);

CREATE INDEX IF NOT EXISTS idx_student_programme_enrolment_student_year
    ON public.student_programme_enrolment (computer_number, academic_year);

CREATE INDEX IF NOT EXISTS idx_exam_session_programme_course_curriculum
    ON public.exam_session_programme_course (programme_course_id, exam_session_id);

-- Backfill programme enrolments when the existing student.program value exactly
-- matches a seeded programme name. Non-matching records are deliberately left
-- untouched and reported by the checks at the bottom of this file.
INSERT INTO public.student_programme_enrolment (
    computer_number,
    programme_id,
    academic_year,
    year_of_study,
    enrolment_status
)
SELECT DISTINCT
    s.computer_number,
    p.programme_id,
    sr.academic_year,
    s.year_of_study,
    CASE
        WHEN s.status = 'SUSPENDED' THEN 'SUSPENDED'
        WHEN s.status = 'DEFERRED' THEN 'DEFERRED'
        WHEN s.status = 'GRADUATED' THEN 'COMPLETED'
        ELSE 'ACTIVE'
    END
FROM public.student s
JOIN public.student_registration sr ON sr.computer_number = s.computer_number
JOIN public.programme p ON lower(trim(p.programme_name)) = lower(trim(s.program))
ON CONFLICT (computer_number, academic_year) DO UPDATE
SET programme_id = EXCLUDED.programme_id,
    year_of_study = EXCLUDED.year_of_study,
    enrolment_status = EXCLUDED.enrolment_status,
    updated_at = CURRENT_TIMESTAMP;

-- Link each existing session to every matching programme curriculum entry for
-- its course and semester. Academic year (for example 2026) is intentionally
-- not compared with year_of_study (for example 2): they mean different things.
INSERT INTO public.exam_session_programme_course (exam_session_id, programme_course_id)
SELECT es.exam_session_id, pc.programme_course_id
FROM public.exam_session es
JOIN public.programme_course pc
    ON pc.course_code = es.course_code
   AND pc.semester = es.semester
   AND pc.is_active = TRUE
ON CONFLICT (exam_session_id, programme_course_id) DO NOTHING;

-- Add foreign keys from the legacy course-code fields to the normalized catalog.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_session_course'
    ) THEN
        ALTER TABLE public.exam_session
            ADD CONSTRAINT fk_exam_session_course
            FOREIGN KEY (course_code) REFERENCES public.course(course_code)
            ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_student_registration_course'
    ) THEN
        ALTER TABLE public.student_registration
            ADD CONSTRAINT fk_student_registration_course
            FOREIGN KEY (course_code) REFERENCES public.course(course_code)
            ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_course_lecturer_course'
    ) THEN
        ALTER TABLE public.course_lecturer
            ADD CONSTRAINT fk_course_lecturer_course
            FOREIGN KEY (course_code) REFERENCES public.course(course_code)
            ON DELETE RESTRICT;
    END IF;

    -- The dump has no FK ensuring a student allocation uses a venue actually
    -- attached to the exam. This makes attendance allocations operationally safe.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_student_venue_allocation_exam_venue'
    ) THEN
        ALTER TABLE public.student_venue_allocation
            ADD CONSTRAINT fk_student_venue_allocation_exam_venue
            FOREIGN KEY (exam_session_id, venue_id)
            REFERENCES public.exam_venue(exam_session_id, venue_id)
            ON DELETE RESTRICT;
    END IF;
END $$;

COMMIT;

-- Run these checks after the transaction succeeds.
-- 1. Students whose legacy programme text did not match a programme record:
-- SELECT s.computer_number, s.program, s.school
-- FROM public.student s
-- LEFT JOIN public.programme p
--   ON lower(trim(p.programme_name)) = lower(trim(s.program))
-- WHERE p.programme_id IS NULL;
--
-- 2. Exam sessions without an academic curriculum mapping:
-- SELECT es.exam_session_id, es.course_code, es.academic_year, es.semester
-- FROM public.exam_session es
-- LEFT JOIN public.exam_session_programme_course espc
--   ON espc.exam_session_id = es.exam_session_id
-- WHERE espc.exam_session_id IS NULL;
--
-- 3. Administrator assignment filter preview:
-- SELECT sch.school_name, p.programme_name, pc.year_of_study, pc.semester,
--        c.course_code, c.course_name, es.exam_session_id, es.exam_date
-- FROM public.exam_session_programme_course espc
-- JOIN public.exam_session es ON es.exam_session_id = espc.exam_session_id
-- JOIN public.programme_course pc ON pc.programme_course_id = espc.programme_course_id
-- JOIN public.programme p ON p.programme_id = pc.programme_id
-- JOIN public.school sch ON sch.school_id = p.school_id
-- JOIN public.course c ON c.course_code = pc.course_code
-- ORDER BY sch.school_name, p.programme_name, pc.year_of_study, pc.semester, es.exam_date;

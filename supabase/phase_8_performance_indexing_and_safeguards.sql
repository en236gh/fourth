-- Phase 8: Performance, indexing, and database safeguards.
--
-- Apply after the academic catalog, operational academic links, and
-- invigilator assignment phases. This script is safe to run repeatedly.
-- The current schema uses course_code on exam_session and programme_course;
-- programme linkage is normalized through exam_session_programme_course.

BEGIN;

-- Academic catalog query paths.
CREATE INDEX IF NOT EXISTS idx_programme_school
    ON public.programme (school_id);

CREATE INDEX IF NOT EXISTS idx_programme_course_programme_year_semester
    ON public.programme_course (programme_id, year_of_study, semester);

CREATE INDEX IF NOT EXISTS idx_programme_course_course
    ON public.programme_course (course_code);

-- Operational academic and examination query paths.
CREATE INDEX IF NOT EXISTS idx_exam_session_course_schedule
    ON public.exam_session (course_code, exam_date, start_time);

CREATE INDEX IF NOT EXISTS idx_exam_session_programme_course_lookup
    ON public.exam_session_programme_course (programme_course_id, exam_session_id);

CREATE INDEX IF NOT EXISTS idx_exam_venue_exam_session
    ON public.exam_venue (exam_session_id);

-- Assignment administration and clash checks.
CREATE INDEX IF NOT EXISTS idx_invigilator_assignment_staff_exam
    ON public.invigilator_assignment (staff_id, exam_session_id);

CREATE INDEX IF NOT EXISTS idx_invigilator_assignment_exam_venue
    ON public.invigilator_assignment (exam_session_id, venue_id);

-- Candidate counts, attendance joins, and venue-level staffing queries.
CREATE INDEX IF NOT EXISTS idx_student_venue_allocation_exam_venue_student
    ON public.student_venue_allocation (exam_session_id, venue_id, computer_number);

CREATE INDEX IF NOT EXISTS idx_student_venue_allocation_exam_student
    ON public.student_venue_allocation (exam_session_id, computer_number);

-- The primary key already covers (computer_number, exam_session_id), which is
-- the direct attendance lookup path.

-- Preserve the existing normalized academic relationships.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'programme_duration_years_check'
    ) THEN
        ALTER TABLE public.programme
            ADD CONSTRAINT programme_duration_years_check
            CHECK (duration_years > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'programme_course_year_of_study_check'
    ) THEN
        ALTER TABLE public.programme_course
            ADD CONSTRAINT programme_course_year_of_study_check
            CHECK (year_of_study > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'programme_course_semester_check'
    ) THEN
        ALTER TABLE public.programme_course
            ADD CONSTRAINT programme_course_semester_check
            CHECK (semester IN (1, 2));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'programme_course_unique_curriculum_entry'
    ) THEN
        ALTER TABLE public.programme_course
            ADD CONSTRAINT programme_course_unique_curriculum_entry
            UNIQUE (programme_id, course_code, year_of_study, semester);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_programme_course_programme'
    ) THEN
        ALTER TABLE public.programme_course
            ADD CONSTRAINT fk_programme_course_programme
            FOREIGN KEY (programme_id) REFERENCES public.programme(programme_id)
            ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_programme_course_course'
    ) THEN
        ALTER TABLE public.programme_course
            ADD CONSTRAINT fk_programme_course_course
            FOREIGN KEY (course_code) REFERENCES public.course(course_code)
            ON DELETE RESTRICT;
    END IF;
END $$;

-- Assignment inserts and status changes remain transactional through the
-- backend service; Phase 6's trigger supplies the database-level overlap
-- protection and advisory lock for concurrent writes.

COMMIT;

-- Validation queries:
-- SELECT indexname, tablename
-- FROM pg_indexes
-- WHERE schemaname = 'public'
--   AND (indexname LIKE '%programme%'
--        OR indexname LIKE '%exam_session%'
--        OR indexname LIKE '%invigilator_assignment%'
--        OR indexname LIKE '%student_venue_allocation%')
-- ORDER BY tablename, indexname;
--
-- SELECT conname, conrelid::regclass
-- FROM pg_constraint
-- WHERE connamespace = 'public'::regnamespace
--   AND conname IN (
--       'programme_duration_years_check',
--       'programme_course_year_of_study_check',
--       'programme_course_semester_check',
--       'programme_course_unique_curriculum_entry',
--       'fk_programme_course_programme',
--       'fk_programme_course_course'
--   );
-- Phase 5: Administrator-managed invigilator assignments.
--
-- Reference schema: exam_attendance.sql plus Phase 2-4 additions.
-- This upgrades the existing public.invigilator_assignment table; it does not
-- replace its correct primary key: (exam_session_id, venue_id, staff_id).
--
-- Run manually in the Supabase SQL Editor after Phase 4.

BEGIN;

-- Each assignment is created as DRAFT and becomes visible operationally only
-- when an administrator publishes it. Existing assignments become DRAFT.
ALTER TABLE public.invigilator_assignment
    ADD COLUMN IF NOT EXISTS assignment_status character varying(20)
        NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS assigned_by_staff_id integer,
    ADD COLUMN IF NOT EXISTS assigned_at timestamp without time zone
        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS published_at timestamp without time zone,
    ADD COLUMN IF NOT EXISTS assignment_notes text;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'invigilator_assignment_status_check'
    ) THEN
        ALTER TABLE public.invigilator_assignment
            ADD CONSTRAINT invigilator_assignment_status_check
            CHECK (assignment_status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'invigilator_assignment_published_at_check'
    ) THEN
        ALTER TABLE public.invigilator_assignment
            ADD CONSTRAINT invigilator_assignment_published_at_check
            CHECK (assignment_status <> 'PUBLISHED' OR published_at IS NOT NULL);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_invigilator_assignment_assigned_by'
    ) THEN
        ALTER TABLE public.invigilator_assignment
            ADD CONSTRAINT fk_invigilator_assignment_assigned_by
            FOREIGN KEY (assigned_by_staff_id)
            REFERENCES public.staff(staff_id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_invigilator_assignment_staff_status
    ON public.invigilator_assignment (staff_id, assignment_status);

CREATE INDEX IF NOT EXISTS idx_invigilator_assignment_exam_venue_status
    ON public.invigilator_assignment (exam_session_id, venue_id, assignment_status);

-- Prevent a lecturer or administrator account from being accidentally assigned
-- as an invigilator. A staff member may have several roles, but must hold the
-- INVIGILATOR role to appear in this table.
CREATE OR REPLACE FUNCTION public.enforce_invigilator_assignment_role()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.staff_role sr
        JOIN public.role r ON r.role_id = sr.role_id
        WHERE sr.staff_id = NEW.staff_id
          AND r.name = 'INVIGILATOR'
    ) THEN
        RAISE EXCEPTION 'Staff ID % does not have the INVIGILATOR role', NEW.staff_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_invigilator_assignment_role
    ON public.invigilator_assignment;

CREATE TRIGGER trg_enforce_invigilator_assignment_role
BEFORE INSERT OR UPDATE OF staff_id ON public.invigilator_assignment
FOR EACH ROW
EXECUTE FUNCTION public.enforce_invigilator_assignment_role();

-- A read model for the future administrator assignment screen. It presents a
-- single row per invigilator duty and aggregates all programmes served by the
-- exam session, so shared-course examinations are not duplicated in the UI.
CREATE OR REPLACE VIEW public.admin_invigilator_assignment_overview AS
SELECT
    ia.exam_session_id,
    es.course_code,
    c.course_name,
    es.academic_year,
    es.semester,
    es.exam_date,
    es.start_time,
    es.end_time,
    es.status AS exam_status,
    ia.venue_id,
    v.venue_name,
    v.building,
    v.capacity AS venue_capacity,
    ia.staff_id AS invigilator_staff_id,
    inv.full_name AS invigilator_name,
    inv.email AS invigilator_email,
    ia.assignment_status,
    ia.assigned_by_staff_id,
    admin.full_name AS assigned_by_name,
    ia.assigned_at,
    ia.published_at,
    ia.assignment_notes,
    COALESCE(
        string_agg(
            DISTINCT sch.school_name || ' / ' || p.programme_name ||
            ' / Year ' || pc.year_of_study || ' / Semester ' || pc.semester,
            '; ' ORDER BY sch.school_name || ' / ' || p.programme_name ||
            ' / Year ' || pc.year_of_study || ' / Semester ' || pc.semester
        ) FILTER (WHERE p.programme_id IS NOT NULL),
        'No programme mapping'
    ) AS academic_scope
FROM public.invigilator_assignment ia
JOIN public.exam_session es ON es.exam_session_id = ia.exam_session_id
JOIN public.course c ON c.course_code = es.course_code
JOIN public.venue v ON v.venue_id = ia.venue_id
JOIN public.staff inv ON inv.staff_id = ia.staff_id
LEFT JOIN public.staff admin ON admin.staff_id = ia.assigned_by_staff_id
LEFT JOIN public.exam_session_programme_course espc
    ON espc.exam_session_id = ia.exam_session_id
LEFT JOIN public.programme_course pc
    ON pc.programme_course_id = espc.programme_course_id
LEFT JOIN public.programme p ON p.programme_id = pc.programme_id
LEFT JOIN public.school sch ON sch.school_id = p.school_id
GROUP BY
    ia.exam_session_id, es.course_code, c.course_name, es.academic_year,
    es.semester, es.exam_date, es.start_time, es.end_time, es.status,
    ia.venue_id, v.venue_name, v.building, v.capacity,
    ia.staff_id, inv.full_name, inv.email, ia.assignment_status,
    ia.assigned_by_staff_id, admin.full_name, ia.assigned_at,
    ia.published_at, ia.assignment_notes;

COMMIT;

-- Manual assignment pattern for the future administrator workflow:
-- INSERT INTO public.invigilator_assignment (
--     exam_session_id, venue_id, staff_id, assignment_status, assigned_by_staff_id
-- ) VALUES (1, 1, 3, 'DRAFT', 1);
--
-- Publish all draft assignments for an exam only after the administrator reviews them:
-- UPDATE public.invigilator_assignment
-- SET assignment_status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP
-- WHERE exam_session_id = 1 AND assignment_status = 'DRAFT';
--
-- Administrator assignment-screen query:
-- SELECT * FROM public.admin_invigilator_assignment_overview
-- WHERE assignment_status <> 'CANCELLED'
-- ORDER BY exam_date, start_time, venue_name, invigilator_name;

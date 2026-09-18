-- Phase 7: Automatic invigilator assignment support.
--
-- Apply phase_5_admin_invigilator_assignment.sql and
-- phase_6_assignment_conflicts_and_staffing.sql before this script.
-- The assignment algorithm runs in the backend so administrators can review
-- generated DRAFT rows before publishing them.

BEGIN;

-- Keep database writes consistent with the automatic assignment eligibility
-- rule, including assignments created outside the backend API.
CREATE OR REPLACE FUNCTION public.enforce_active_invigilator_assignment()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.staff s
        JOIN public.staff_role sr ON sr.staff_id = s.staff_id
        JOIN public.role r ON r.role_id = sr.role_id
        WHERE s.staff_id = NEW.staff_id
          AND s.account_status = 'ACTIVE'
          AND r.name = 'INVIGILATOR'
    ) THEN
        RAISE EXCEPTION 'Staff ID % is not an active invigilator', NEW.staff_id;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_active_invigilator_assignment
    ON public.invigilator_assignment;

CREATE TRIGGER trg_enforce_active_invigilator_assignment
BEFORE INSERT OR UPDATE OF staff_id
ON public.invigilator_assignment
FOR EACH ROW
EXECUTE FUNCTION public.enforce_active_invigilator_assignment();

-- Read model used by administrator workload dashboards and audit views.
CREATE OR REPLACE VIEW public.admin_invigilator_workload_overview AS
SELECT
    s.staff_id,
    s.full_name,
    s.email,
    COUNT(ia.staff_id) FILTER (WHERE ia.assignment_status <> 'CANCELLED')
        AS active_duty_count,
    COUNT(ia.staff_id) FILTER (WHERE ia.assignment_status = 'DRAFT')
        AS draft_duty_count,
    COUNT(ia.staff_id) FILTER (WHERE ia.assignment_status = 'PUBLISHED')
        AS published_duty_count
FROM public.staff s
JOIN public.staff_role sr ON sr.staff_id = s.staff_id
JOIN public.role r ON r.role_id = sr.role_id AND r.name = 'INVIGILATOR'
LEFT JOIN public.invigilator_assignment ia ON ia.staff_id = s.staff_id
WHERE s.account_status = 'ACTIVE'
GROUP BY s.staff_id, s.full_name, s.email;

CREATE INDEX IF NOT EXISTS idx_invigilator_assignment_exam_status
    ON public.invigilator_assignment (exam_session_id, assignment_status);

COMMIT;

-- Backend endpoint flow:
-- GET  /api/admin/invigilator-assignments
-- GET  /api/admin/invigilator-assignments/exam-sessions/{id}/staffing
-- POST /api/admin/invigilator-assignments/exam-sessions/{id}/auto-assign
-- POST /api/admin/invigilator-assignments/{exam}/{venue}/{staff}/cancel
-- POST /api/admin/invigilator-assignments/exam-sessions/{id}/publish
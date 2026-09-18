-- Phase 6: Assignment conflict prevention and venue staffing visibility.
--
-- Run manually in the Supabase SQL Editor after Phase 5.
-- This script prevents one invigilator being assigned to overlapping exam
-- sessions, including draft assignments. Cancelled assignments are ignored.

BEGIN;

CREATE INDEX IF NOT EXISTS idx_student_venue_allocation_exam_venue
    ON public.student_venue_allocation (exam_session_id, venue_id);

-- Stops a new or edited assignment from double-booking an invigilator.
CREATE OR REPLACE FUNCTION public.enforce_invigilator_schedule_conflict()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    target_exam record;
    conflicting_exam record;
BEGIN
    -- A cancelled duty cannot conflict with another duty.
    IF NEW.assignment_status = 'CANCELLED' THEN
        RETURN NEW;
    END IF;

    -- Serialize assignment edits per staff member, including concurrent admin
    -- requests, so two transactions cannot create a double booking together.
    PERFORM pg_advisory_xact_lock(NEW.staff_id);

    SELECT exam_date, start_time, end_time, status
    INTO target_exam
    FROM public.exam_session
    WHERE exam_session_id = NEW.exam_session_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Exam session % does not exist', NEW.exam_session_id;
    END IF;

    IF target_exam.status = 'COMPLETED' THEN
        RAISE EXCEPTION 'Cannot assign staff to completed exam session %', NEW.exam_session_id;
    END IF;

    SELECT es.exam_session_id, es.course_code, es.exam_date, es.start_time, es.end_time
    INTO conflicting_exam
    FROM public.invigilator_assignment ia
    JOIN public.exam_session es ON es.exam_session_id = ia.exam_session_id
    WHERE ia.staff_id = NEW.staff_id
      AND ia.assignment_status <> 'CANCELLED'
      AND (ia.exam_session_id, ia.venue_id, ia.staff_id)
          IS DISTINCT FROM (NEW.exam_session_id, NEW.venue_id, NEW.staff_id)
      AND es.exam_date = target_exam.exam_date
      AND es.start_time < target_exam.end_time
      AND es.end_time > target_exam.start_time
    LIMIT 1;

    IF FOUND THEN
        RAISE EXCEPTION
            'Invigilator staff ID % already has a conflicting assignment: exam % (%) on % from % to %',
            NEW.staff_id,
            conflicting_exam.exam_session_id,
            conflicting_exam.course_code,
            conflicting_exam.exam_date,
            conflicting_exam.start_time,
            conflicting_exam.end_time;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_invigilator_schedule_conflict
    ON public.invigilator_assignment;

CREATE TRIGGER trg_enforce_invigilator_schedule_conflict
BEFORE INSERT OR UPDATE OF exam_session_id, venue_id, staff_id, assignment_status
ON public.invigilator_assignment
FOR EACH ROW
EXECUTE FUNCTION public.enforce_invigilator_schedule_conflict();

-- Rescheduling an exam must also be rejected if it would create an overlap for
-- any already assigned invigilator.
CREATE OR REPLACE FUNCTION public.enforce_exam_reschedule_conflict()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    staff_record record;
    conflicting_exam record;
BEGIN
    IF NEW.exam_date = OLD.exam_date
       AND NEW.start_time = OLD.start_time
       AND NEW.end_time = OLD.end_time THEN
        RETURN NEW;
    END IF;

    FOR staff_record IN
        SELECT DISTINCT staff_id
        FROM public.invigilator_assignment
        WHERE exam_session_id = NEW.exam_session_id
          AND assignment_status <> 'CANCELLED'
        ORDER BY staff_id
    LOOP
        PERFORM pg_advisory_xact_lock(staff_record.staff_id);

        SELECT es.exam_session_id, es.course_code, es.exam_date, es.start_time, es.end_time
        INTO conflicting_exam
        FROM public.invigilator_assignment ia
        JOIN public.exam_session es ON es.exam_session_id = ia.exam_session_id
        WHERE ia.staff_id = staff_record.staff_id
          AND ia.assignment_status <> 'CANCELLED'
          AND ia.exam_session_id <> NEW.exam_session_id
          AND es.exam_date = NEW.exam_date
          AND es.start_time < NEW.end_time
          AND es.end_time > NEW.start_time
        LIMIT 1;

        IF FOUND THEN
            RAISE EXCEPTION
                'Cannot reschedule exam %: invigilator staff ID % conflicts with exam % (%) on % from % to %',
                NEW.exam_session_id,
                staff_record.staff_id,
                conflicting_exam.exam_session_id,
                conflicting_exam.course_code,
                conflicting_exam.exam_date,
                conflicting_exam.start_time,
                conflicting_exam.end_time;
        END IF;
    END LOOP;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_exam_reschedule_conflict
    ON public.exam_session;

CREATE TRIGGER trg_enforce_exam_reschedule_conflict
BEFORE UPDATE OF exam_date, start_time, end_time
ON public.exam_session
FOR EACH ROW
EXECUTE FUNCTION public.enforce_exam_reschedule_conflict();

-- Staffing read model. The initial policy is one invigilator per 50 allocated
-- students, with at least one invigilator for every active exam venue.
CREATE OR REPLACE VIEW public.admin_exam_venue_staffing_overview AS
SELECT
    es.exam_session_id,
    es.course_code,
    c.course_name,
    es.exam_date,
    es.start_time,
    es.end_time,
    es.status AS exam_status,
    ev.venue_id,
    v.venue_name,
    v.building,
    v.capacity AS venue_capacity,
    COUNT(DISTINCT sva.computer_number) AS allocated_student_count,
    GREATEST(
        1,
        CEIL(COUNT(DISTINCT sva.computer_number)::numeric / 50)::integer
    ) AS required_invigilator_count,
    COUNT(DISTINCT ia.staff_id)
        FILTER (WHERE ia.assignment_status = 'DRAFT') AS draft_invigilator_count,
    COUNT(DISTINCT ia.staff_id)
        FILTER (WHERE ia.assignment_status = 'PUBLISHED') AS published_invigilator_count,
    CASE
        WHEN COUNT(DISTINCT ia.staff_id)
            FILTER (WHERE ia.assignment_status = 'PUBLISHED')
            >= GREATEST(1, CEIL(COUNT(DISTINCT sva.computer_number)::numeric / 50)::integer)
        THEN 'STAFFED'
        ELSE 'UNDERSTAFFED'
    END AS staffing_status
FROM public.exam_venue ev
JOIN public.exam_session es ON es.exam_session_id = ev.exam_session_id
JOIN public.course c ON c.course_code = es.course_code
JOIN public.venue v ON v.venue_id = ev.venue_id
LEFT JOIN public.student_venue_allocation sva
    ON sva.exam_session_id = ev.exam_session_id
   AND sva.venue_id = ev.venue_id
LEFT JOIN public.invigilator_assignment ia
    ON ia.exam_session_id = ev.exam_session_id
   AND ia.venue_id = ev.venue_id
   AND ia.assignment_status <> 'CANCELLED'
GROUP BY
    es.exam_session_id, es.course_code, c.course_name, es.exam_date,
    es.start_time, es.end_time, es.status, ev.venue_id, v.venue_name,
    v.building, v.capacity;

COMMIT;

-- Review staffing before publishing assignments:
-- SELECT * FROM public.admin_exam_venue_staffing_overview
-- WHERE staffing_status = 'UNDERSTAFFED'
-- ORDER BY exam_date, start_time, venue_name;
--
-- Conflict test: this should fail if staff ID 3 is already assigned to an
-- overlapping non-cancelled examination:
-- INSERT INTO public.invigilator_assignment (
--     exam_session_id, venue_id, staff_id, assignment_status, assigned_by_staff_id
-- ) VALUES (2, 1, 3, 'DRAFT', 1);

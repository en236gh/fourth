-- Apply once after V34. Ordinary publication locks remain in force.
-- This workflow supports date/time corrections only; venues, eligibility and attendance are unchanged.
CREATE TABLE public.examination_amendment (
    amendment_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    period_id integer NOT NULL REFERENCES public.examination_period,
    exam_session_id integer NOT NULL REFERENCES public.exam_session,
    proposed_by_staff_id integer NOT NULL REFERENCES public.staff,
    expected_revision bigint NOT NULL,
    exam_date date NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL CHECK(end_time>start_time),
    reason varchar(2000) NOT NULL CHECK(length(trim(reason))>0),
    status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','APPLIED')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_staff_id integer REFERENCES public.staff,
    reviewed_at timestamptz,
    decision varchar(2000),
    applied_at timestamptz,
    before_arrangement jsonb,
    after_arrangement jsonb
);
CREATE TABLE public.examination_notification (
    notification_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    amendment_id bigint NOT NULL REFERENCES public.examination_amendment,
    recipient_kind varchar(10) NOT NULL CHECK(recipient_kind IN ('STAFF','STUDENT')),
    recipient_id varchar(30) NOT NULL,
    message text NOT NULL,
    delivery_status varchar(15) NOT NULL DEFAULT 'PENDING' CHECK(delivery_status IN ('PENDING','AVAILABLE','FAILED')),
    attempts integer NOT NULL DEFAULT 0,
    last_error text,
    available_at timestamptz,
    read_at timestamptz,
    UNIQUE(amendment_id,recipient_kind,recipient_id)
);
CREATE TRIGGER scheduling_audit AFTER INSERT OR UPDATE OR DELETE ON public.examination_amendment
    FOR EACH ROW EXECUTE FUNCTION public.audit_scheduling_change();

-- Narrow exception: only the exact approved date/time, on the exact unstarted exam.
-- Allocation, booking and registration locks are not changed.
CREATE OR REPLACE FUNCTION public.protect_published_exam() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE pid integer;
BEGIN
    IF TG_TABLE_NAME='exam_session' THEN
        pid:=OLD.period_id;
        IF TG_OP='UPDATE' AND (NEW.course_code,NEW.exam_date,NEW.start_time,NEW.end_time,NEW.academic_year,NEW.semester,NEW.exam_type,NEW.period_id,NEW.schedule_published)
            IS NOT DISTINCT FROM (OLD.course_code,OLD.exam_date,OLD.start_time,OLD.end_time,OLD.academic_year,OLD.semester,OLD.exam_type,OLD.period_id,OLD.schedule_published) THEN RETURN NEW; END IF;
        IF TG_OP='UPDATE' AND OLD.status='SCHEDULED' AND NEW.status=OLD.status
            AND (NEW.course_code,NEW.academic_year,NEW.semester,NEW.exam_type,NEW.period_id,NEW.schedule_published)
            IS NOT DISTINCT FROM (OLD.course_code,OLD.academic_year,OLD.semester,OLD.exam_type,OLD.period_id,OLD.schedule_published)
            AND EXISTS (
                SELECT 1 FROM examination_amendment a JOIN examination_period p USING(period_id)
                JOIN scheduling_lead_permission lead ON lead.staff_id=a.reviewed_by_staff_id
                WHERE a.amendment_id=NULLIF(current_setting('app.scheduling_amendment',true),'')::bigint
                AND a.exam_session_id=OLD.exam_session_id AND a.period_id=OLD.period_id AND a.status='APPROVED'
                AND a.expected_revision=p.revision AND p.status='PUBLISHED'
                AND p.coordinator_staff_id=NULLIF(current_setting('app.scheduling_actor',true),'')::integer
                AND (NEW.exam_date,NEW.start_time,NEW.end_time)=(a.exam_date,a.start_time,a.end_time)
                AND OLD.exam_date+OLD.start_time > CURRENT_TIMESTAMP AT TIME ZONE p.timezone
                AND NEW.exam_date+NEW.start_time > CURRENT_TIMESTAMP AT TIME ZONE p.timezone
            ) AND NOT EXISTS(SELECT 1 FROM attendance WHERE exam_session_id=OLD.exam_session_id)
              AND NOT EXISTS(SELECT 1 FROM incident WHERE exam_session_id=OLD.exam_session_id)
              AND NOT EXISTS(SELECT 1 FROM generated_report WHERE exam_session_id=OLD.exam_session_id)
        THEN RETURN NEW; END IF;
    ELSE
        SELECT period_id INTO pid FROM public.exam_session WHERE exam_session_id=OLD.exam_session_id;
    END IF;
    IF EXISTS(SELECT 1 FROM public.examination_period WHERE period_id=pid AND status='PUBLISHED') THEN
        RAISE EXCEPTION 'Published examination allocations and placements are locked';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;

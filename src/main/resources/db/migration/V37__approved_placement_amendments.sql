-- Apply once after V36. Expands the approval-scoped amendment to venues and duties.
-- Exam identity, registration and attendance history are never replaced.
CREATE TABLE public.examination_amendment_venue (
    amendment_id bigint NOT NULL REFERENCES public.examination_amendment,
    venue_id integer NOT NULL REFERENCES public.venue,
    PRIMARY KEY(amendment_id,venue_id)
);
CREATE TABLE public.examination_amendment_allocation (
    amendment_id bigint NOT NULL,
    computer_number varchar(15) NOT NULL REFERENCES public.student,
    venue_id integer NOT NULL,
    PRIMARY KEY(amendment_id,computer_number),
    FOREIGN KEY(amendment_id,venue_id) REFERENCES public.examination_amendment_venue
);
CREATE TABLE public.examination_amendment_duty (
    amendment_id bigint NOT NULL,
    venue_id integer NOT NULL,
    staff_id integer NOT NULL REFERENCES public.staff,
    PRIMARY KEY(amendment_id,venue_id,staff_id),
    UNIQUE(amendment_id,staff_id),
    FOREIGN KEY(amendment_id,venue_id) REFERENCES public.examination_amendment_venue
);
-- Preserve the exact scope of proposals created under the time-only version.
INSERT INTO examination_amendment_venue SELECT a.amendment_id,v.venue_id FROM examination_amendment a JOIN exam_venue v USING(exam_session_id);
INSERT INTO examination_amendment_allocation SELECT a.amendment_id,v.computer_number,v.venue_id FROM examination_amendment a JOIN student_venue_allocation v USING(exam_session_id);
INSERT INTO examination_amendment_duty SELECT a.amendment_id,d.venue_id,d.staff_id FROM examination_amendment a JOIN invigilator_assignment d USING(exam_session_id) WHERE d.assignment_status='PUBLISHED';

CREATE FUNCTION public.approved_amendment_for(target integer) RETURNS bigint LANGUAGE sql STABLE AS $$
    SELECT a.amendment_id FROM examination_amendment a JOIN examination_period p USING(period_id)
    JOIN exam_session e ON e.exam_session_id=a.exam_session_id AND e.period_id=p.period_id
    JOIN scheduling_lead_permission l ON l.staff_id=a.reviewed_by_staff_id
    JOIN staff reviewer ON reviewer.staff_id=l.staff_id AND reviewer.account_status='ACTIVE'
    WHERE a.amendment_id=NULLIF(current_setting('app.scheduling_amendment',true),'')::bigint
    AND a.exam_session_id=target AND a.status='APPROVED' AND p.status='PUBLISHED' AND p.revision=a.expected_revision
    AND p.coordinator_staff_id=NULLIF(current_setting('app.scheduling_actor',true),'')::integer
    AND e.status='SCHEDULED' AND e.exam_date+e.start_time>CURRENT_TIMESTAMP AT TIME ZONE p.timezone
    AND a.exam_date+a.start_time>CURRENT_TIMESTAMP AT TIME ZONE p.timezone
    AND EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=reviewer.staff_id AND r.name='ADMINISTRATOR')
    AND NOT EXISTS(SELECT 1 FROM attendance WHERE exam_session_id=target)
    AND NOT EXISTS(SELECT 1 FROM incident WHERE exam_session_id=target)
    AND NOT EXISTS(SELECT 1 FROM generated_report WHERE exam_session_id=target)
$$;
CREATE OR REPLACE FUNCTION public.protect_published_exam() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE pid integer; aid bigint;
BEGIN
    IF TG_TABLE_NAME='exam_session' THEN
        pid:=OLD.period_id;
        IF TG_OP='UPDATE' AND (to_jsonb(NEW)-'status')=(to_jsonb(OLD)-'status') THEN RETURN NEW; END IF;
        aid:=approved_amendment_for(OLD.exam_session_id);
        IF aid IS NOT NULL AND TG_OP='UPDATE'
            AND (to_jsonb(NEW)-ARRAY['exam_date','start_time','end_time'])=(to_jsonb(OLD)-ARRAY['exam_date','start_time','end_time'])
            AND EXISTS(SELECT 1 FROM examination_amendment a WHERE a.amendment_id=aid AND (NEW.exam_date,NEW.start_time,NEW.end_time)=(a.exam_date,a.start_time,a.end_time)) THEN RETURN NEW; END IF;
    ELSE
        SELECT period_id INTO pid FROM exam_session WHERE exam_session_id=OLD.exam_session_id;
        aid:=approved_amendment_for(OLD.exam_session_id);
        IF aid IS NOT NULL THEN
            IF TG_TABLE_NAME='exam_venue' AND TG_OP='DELETE' AND NOT EXISTS(SELECT 1 FROM examination_amendment_venue WHERE amendment_id=aid AND venue_id=OLD.venue_id) THEN RETURN OLD; END IF;
            IF TG_TABLE_NAME='student_venue_allocation' AND TG_OP='UPDATE'
                AND (to_jsonb(NEW)-'venue_id')=(to_jsonb(OLD)-'venue_id')
                AND EXISTS(SELECT 1 FROM examination_amendment_allocation WHERE amendment_id=aid AND computer_number=NEW.computer_number AND venue_id=NEW.venue_id) THEN RETURN NEW; END IF;
        END IF;
    END IF;
    IF EXISTS(SELECT 1 FROM examination_period WHERE period_id=pid AND status='PUBLISHED') THEN RAISE EXCEPTION 'Published placements and allocations require an approved amendment'; END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;

CREATE OR REPLACE FUNCTION public.validate_exam_allocation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE e public.exam_session; cap INTEGER; used INTEGER;
BEGIN
    SELECT * INTO e FROM public.exam_session WHERE exam_session_id=NEW.exam_session_id;
    IF EXISTS (SELECT 1 FROM examination_period WHERE period_id=e.period_id AND status='PUBLISHED')
        AND NOT EXISTS(SELECT 1 FROM examination_amendment_allocation WHERE amendment_id=approved_amendment_for(NEW.exam_session_id) AND venue_id=NEW.venue_id AND computer_number=NEW.computer_number) THEN
        RAISE EXCEPTION 'Published examination allocations are locked';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM public.student_registration r WHERE r.computer_number=NEW.computer_number
        AND r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester) THEN
        RAISE EXCEPTION 'Student is not registered for this course examination';
    END IF;
    SELECT examination_capacity INTO cap FROM public.venue WHERE venue_id=NEW.venue_id;
    IF e.period_id IS NOT NULL AND cap IS NULL THEN RAISE EXCEPTION 'Examination seating capacity is not configured'; END IF;
    IF cap IS NOT NULL THEN
        SELECT count(*) INTO used FROM public.student_venue_allocation
        WHERE exam_session_id=NEW.exam_session_id AND venue_id=NEW.venue_id AND computer_number<>NEW.computer_number;
        IF used >= cap THEN RAISE EXCEPTION 'Examination venue capacity exceeded'; END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.validate_exam_booking() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE e public.exam_session;
BEGIN
    SELECT * INTO e FROM public.exam_session WHERE exam_session_id=NEW.exam_session_id;
    IF EXISTS (SELECT 1 FROM examination_period WHERE period_id=e.period_id AND status='PUBLISHED')
        AND NOT EXISTS(SELECT 1 FROM examination_amendment_venue WHERE amendment_id=approved_amendment_for(NEW.exam_session_id) AND venue_id=NEW.venue_id) THEN
        RAISE EXCEPTION 'Published examination bookings are locked';
    END IF;
    IF EXISTS (SELECT 1 FROM exam_venue b JOIN exam_session other USING(exam_session_id)
        WHERE b.venue_id=NEW.venue_id AND b.exam_session_id<>NEW.exam_session_id
        AND e.exam_date+e.start_time<other.exam_date+other.end_time
        AND e.exam_date+e.end_time>other.exam_date+other.start_time)
      OR EXISTS (SELECT 1 FROM venue_unavailability b WHERE b.venue_id=NEW.venue_id
        AND e.exam_date+e.start_time<b.ends_at AND e.exam_date+e.end_time>b.starts_at) THEN
        RAISE EXCEPTION 'Venue has an overlapping booking or unavailable interval';
    END IF;
    RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.protect_published_staffing() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE aid bigint;
BEGIN
    IF TG_OP<>'INSERT' AND EXISTS(SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id) WHERE e.exam_session_id=OLD.exam_session_id AND p.status='PUBLISHED') THEN
        aid:=approved_amendment_for(OLD.exam_session_id);
        IF aid IS NULL OR TG_OP<>'DELETE' THEN RAISE EXCEPTION 'Published staffing is locked'; END IF;
    END IF;
    IF TG_OP<>'DELETE' AND EXISTS(SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id) WHERE e.exam_session_id=NEW.exam_session_id AND p.status='PUBLISHED') THEN
        aid:=approved_amendment_for(NEW.exam_session_id);
        IF aid IS NULL OR NEW.assignment_status<>'PUBLISHED' OR NOT EXISTS(SELECT 1 FROM examination_amendment_duty WHERE amendment_id=aid AND venue_id=NEW.venue_id AND staff_id=NEW.staff_id) THEN
            RAISE EXCEPTION 'Published staffing requires the approved amendment duty';
        END IF;
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;

-- Child proposals are immutable once reviewed. API inserts them with the pending proposal.
CREATE FUNCTION public.protect_amendment_scope() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE aid bigint;
BEGIN
    IF TG_OP='INSERT' THEN aid:=NEW.amendment_id; ELSE aid:=OLD.amendment_id; END IF;
    IF EXISTS(SELECT 1 FROM examination_amendment WHERE amendment_id=aid AND status<>'PENDING') THEN RAISE EXCEPTION 'Reviewed amendment scope is immutable'; END IF;
    IF TG_OP='UPDATE' AND EXISTS(SELECT 1 FROM examination_amendment WHERE amendment_id=NEW.amendment_id AND status<>'PENDING') THEN RAISE EXCEPTION 'Reviewed amendment scope is immutable'; END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
DO $$ DECLARE t text; BEGIN
    FOREACH t IN ARRAY ARRAY['examination_amendment_venue','examination_amendment_allocation','examination_amendment_duty'] LOOP
        EXECUTE format('CREATE TRIGGER protect_amendment_scope BEFORE INSERT OR UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.protect_amendment_scope()',t);
        EXECUTE format('CREATE TRIGGER schedule_write_lock BEFORE INSERT OR UPDATE OR DELETE ON public.%I FOR EACH STATEMENT EXECUTE FUNCTION public.lock_exam_schedule_writes()',t);
    END LOOP;
END $$;

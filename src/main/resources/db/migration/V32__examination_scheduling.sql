-- Additive migration. Legacy sessions retain their IDs and published visibility.
CREATE TABLE public.examination_period (
    period_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    academic_year VARCHAR(9) NOT NULL,
    semester SMALLINT NOT NULL CHECK (semester IN (1,2)),
    exam_type VARCHAR(20) NOT NULL CHECK (exam_type IN ('FINAL','SUPPLEMENTARY','SPECIAL')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL CHECK (end_date >= start_date AND end_date - start_date <= 366),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Africa/Lusaka',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED')),
    revision BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    UNIQUE (academic_year, semester, exam_type)
);
CREATE TABLE public.examination_period_slot (
    period_id INTEGER NOT NULL REFERENCES public.examination_period ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL CHECK (end_time > start_time),
    PRIMARY KEY (period_id, day_of_week, start_time)
);
CREATE TABLE public.examination_period_course (
    period_id INTEGER NOT NULL REFERENCES public.examination_period ON DELETE CASCADE,
    course_code VARCHAR(15) NOT NULL REFERENCES public.course(course_code),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes BETWEEN 1 AND 1440),
    PRIMARY KEY (period_id, course_code)
);
ALTER TABLE public.exam_session ADD COLUMN period_id INTEGER REFERENCES public.examination_period;
ALTER TABLE public.exam_session ADD COLUMN schedule_published BOOLEAN NOT NULL DEFAULT TRUE;
CREATE UNIQUE INDEX exam_session_period_course_unique ON public.exam_session(period_id, course_code) WHERE period_id IS NOT NULL;
CREATE INDEX exam_session_period_idx ON public.exam_session(period_id);
CREATE INDEX exam_session_interval_idx ON public.exam_session(exam_date, start_time, end_time);
ALTER TABLE public.venue ADD COLUMN examination_capacity INTEGER CHECK (examination_capacity > 0);
-- NULL means not yet verified for examination seating. Never copy classroom capacity implicitly.
CREATE TABLE public.venue_unavailability (
    unavailability_id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    venue_id INTEGER NOT NULL REFERENCES public.venue,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL CHECK (ends_at > starts_at),
    reason VARCHAR(500) NOT NULL
);
CREATE INDEX venue_unavailability_interval_idx ON public.venue_unavailability(venue_id, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS allocation_exam_venue_idx ON public.student_venue_allocation(exam_session_id, venue_id);
-- NOT VALID preserves ambiguous historic rows; all subsequent writes are checked.
ALTER TABLE public.student_venue_allocation ADD CONSTRAINT allocation_exam_booking_fk
    FOREIGN KEY (exam_session_id, venue_id) REFERENCES public.exam_venue(exam_session_id, venue_id) NOT VALID;

CREATE VIEW public.exam_allocation_audit AS
SELECT a.computer_number, a.exam_session_id, a.venue_id, e.course_code,
       CASE WHEN e.exam_session_id IS NULL THEN 'MISSING_EXAM'
            WHEN NOT EXISTS (SELECT 1 FROM public.exam_venue v WHERE v.exam_session_id=a.exam_session_id AND v.venue_id=a.venue_id)
                THEN 'MISSING_EXAM_VENUE'
            WHEN NOT EXISTS (SELECT 1 FROM public.student_registration r WHERE r.computer_number=a.computer_number
                AND r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester)
                THEN 'NOT_REGISTERED_FOR_EXAM' END AS issue
FROM public.student_venue_allocation a LEFT JOIN public.exam_session e USING(exam_session_id)
WHERE e.exam_session_id IS NULL
   OR NOT EXISTS (SELECT 1 FROM public.exam_venue v WHERE v.exam_session_id=a.exam_session_id AND v.venue_id=a.venue_id)
   OR NOT EXISTS (SELECT 1 FROM public.student_registration r WHERE r.computer_number=a.computer_number
       AND r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester);

-- The same transaction lock is used by the scheduling and invigilation services.
-- Statement triggers serialize even direct SQL writers before their row locks.
CREATE FUNCTION public.lock_exam_schedule_writes() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(741902, 1);
    RETURN NULL;
END $$;
DO $$ DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['exam_session','exam_venue','student_venue_allocation',
        'student_registration','venue','venue_unavailability','invigilator_assignment','examination_period',
        'examination_period_course','examination_period_slot','staff','staff_role'] LOOP
        EXECUTE format('CREATE TRIGGER schedule_write_lock BEFORE INSERT OR UPDATE OR DELETE ON public.%I FOR EACH STATEMENT EXECUTE FUNCTION public.lock_exam_schedule_writes()', t);
    END LOOP;
END $$;

CREATE FUNCTION public.validate_exam_allocation() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE e public.exam_session; cap INTEGER; used INTEGER;
BEGIN
    SELECT * INTO e FROM public.exam_session WHERE exam_session_id=NEW.exam_session_id;
    IF EXISTS (SELECT 1 FROM examination_period WHERE period_id=e.period_id AND status='PUBLISHED') THEN
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
CREATE TRIGGER validate_exam_allocation BEFORE INSERT OR UPDATE ON public.student_venue_allocation
    FOR EACH ROW EXECUTE FUNCTION public.validate_exam_allocation();

-- Published managed timetables are immutable through ordinary table writes too.
-- Status transitions and attendance remain permitted; legacy data is preserved.
CREATE FUNCTION public.protect_published_exam() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE pid INTEGER;
BEGIN
    IF TG_TABLE_NAME='exam_session' THEN
        pid := OLD.period_id;
        IF TG_OP='UPDATE' AND (NEW.course_code,NEW.exam_date,NEW.start_time,NEW.end_time,NEW.academic_year,NEW.semester,NEW.exam_type,NEW.period_id,NEW.schedule_published)
             IS NOT DISTINCT FROM (OLD.course_code,OLD.exam_date,OLD.start_time,OLD.end_time,OLD.academic_year,OLD.semester,OLD.exam_type,OLD.period_id,OLD.schedule_published) THEN RETURN NEW; END IF;
    ELSE
        SELECT period_id INTO pid FROM public.exam_session WHERE exam_session_id=OLD.exam_session_id;
    END IF;
    IF EXISTS (SELECT 1 FROM public.examination_period WHERE period_id=pid AND status='PUBLISHED') THEN
        RAISE EXCEPTION 'Published examination allocations and placements are locked';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER protect_published_exam BEFORE UPDATE OR DELETE ON public.exam_session FOR EACH ROW EXECUTE FUNCTION public.protect_published_exam();
CREATE TRIGGER protect_published_booking BEFORE UPDATE OR DELETE ON public.exam_venue FOR EACH ROW EXECUTE FUNCTION public.protect_published_exam();
CREATE TRIGGER protect_published_allocation BEFORE UPDATE OR DELETE ON public.student_venue_allocation FOR EACH ROW EXECUTE FUNCTION public.protect_published_exam();


CREATE FUNCTION public.validate_exam_booking() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE e public.exam_session;
BEGIN
    SELECT * INTO e FROM public.exam_session WHERE exam_session_id=NEW.exam_session_id;
    IF EXISTS (SELECT 1 FROM examination_period WHERE period_id=e.period_id AND status='PUBLISHED') THEN
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
CREATE TRIGGER validate_exam_booking BEFORE INSERT OR UPDATE ON public.exam_venue FOR EACH ROW EXECUTE FUNCTION public.validate_exam_booking();

CREATE FUNCTION public.protect_exam_registration() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE r public.student_registration;
BEGIN
    IF TG_OP='INSERT' THEN r:=NEW; ELSE r:=OLD; END IF;
    IF EXISTS (SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id)
        WHERE p.status='PUBLISHED' AND e.course_code=r.course_code AND e.academic_year=r.academic_year AND e.semester=r.semester) THEN
        RAISE EXCEPTION 'Registrations for a published examination cycle are locked';
    END IF;
    IF TG_OP='UPDATE' AND EXISTS (SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id)
        WHERE p.status='PUBLISHED' AND e.course_code=NEW.course_code AND e.academic_year=NEW.academic_year AND e.semester=NEW.semester) THEN
        RAISE EXCEPTION 'Registrations for a published examination cycle are locked';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER protect_exam_registration BEFORE INSERT OR UPDATE OR DELETE ON public.student_registration FOR EACH ROW EXECUTE FUNCTION public.protect_exam_registration();

CREATE FUNCTION public.validate_exam_capacity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.examination_capacity IS DISTINCT FROM OLD.examination_capacity AND EXISTS (
        SELECT 1 FROM student_venue_allocation a JOIN exam_session e USING(exam_session_id)
        WHERE a.venue_id=NEW.venue_id AND (NEW.examination_capacity IS NOT NULL OR e.period_id IS NOT NULL)
        GROUP BY a.exam_session_id HAVING NEW.examination_capacity IS NULL OR count(*)>NEW.examination_capacity) THEN
        RAISE EXCEPTION 'Examination capacity cannot be reduced below existing allocations';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER validate_exam_capacity BEFORE UPDATE ON public.venue FOR EACH ROW EXECUTE FUNCTION public.validate_exam_capacity();

ALTER TABLE public.exam_session ADD CONSTRAINT exam_selected_course_fk
 FOREIGN KEY(period_id,course_code) REFERENCES public.examination_period_course(period_id,course_code);

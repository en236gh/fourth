-- Apply once, in one transaction, after V32. No ownership or lead permission is guessed.
ALTER TABLE public.examination_period ADD COLUMN coordinator_staff_id integer REFERENCES public.staff(staff_id);
CREATE TABLE public.scheduling_lead_permission (
    staff_id integer PRIMARY KEY REFERENCES public.staff(staff_id),
    granted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    grant_reason text NOT NULL CHECK (length(trim(grant_reason)) > 0)
);
COMMENT ON TABLE public.scheduling_lead_permission IS 'Explicitly provisioned lead permission; no automatic administrator grants.';
CREATE TABLE public.scheduling_audit (
    audit_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_staff_id integer,
    period_id integer,
    action text NOT NULL,
    entity text NOT NULL,
    before_record jsonb,
    after_record jsonb
);
CREATE INDEX scheduling_audit_period_idx ON public.scheduling_audit(period_id,audit_id);
CREATE FUNCTION public.audit_scheduling_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE old_row jsonb; new_row jsonb; pid integer; sid integer;
BEGIN
    IF TG_OP <> 'INSERT' THEN old_row := to_jsonb(OLD); END IF;
    IF TG_OP <> 'DELETE' THEN new_row := to_jsonb(NEW); END IF;
    pid := COALESCE((new_row->>'period_id')::integer,(old_row->>'period_id')::integer);
    sid := COALESCE((new_row->>'exam_session_id')::integer,(old_row->>'exam_session_id')::integer);
    IF pid IS NULL AND sid IS NOT NULL THEN SELECT period_id INTO pid FROM public.exam_session WHERE exam_session_id=sid; END IF;
    INSERT INTO public.scheduling_audit(actor_staff_id,period_id,action,entity,before_record,after_record)
    VALUES (NULLIF(current_setting('app.scheduling_actor',true),'')::integer,pid,TG_OP,TG_TABLE_NAME,old_row,new_row);
    RETURN NULL;
END $$;
DO $$ DECLARE t text; BEGIN
    FOREACH t IN ARRAY ARRAY['examination_period','examination_period_slot','examination_period_course',
        'exam_session','exam_venue','student_venue_allocation','invigilator_assignment',
        'venue','venue_unavailability','scheduling_lead_permission'] LOOP
        EXECUTE format('CREATE TRIGGER scheduling_audit AFTER INSERT OR UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.audit_scheduling_change()',t);
    END LOOP;
END $$;

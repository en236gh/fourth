-- Apply once after V35 in a single transaction. No existing rows are changed.
-- Period ownership/revision metadata may change after publication; setup cannot.
CREATE FUNCTION public.protect_published_period_setup() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE pid integer;
BEGIN
    IF TG_TABLE_NAME='examination_period' THEN
        IF OLD.status='PUBLISHED' THEN
            IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Published periods cannot be deleted'; END IF;
            IF (to_jsonb(NEW)-ARRAY['coordinator_staff_id','revision']) IS DISTINCT FROM
               (to_jsonb(OLD)-ARRAY['coordinator_staff_id','revision']) THEN
                RAISE EXCEPTION 'Published period setup is locked';
            END IF;
        END IF;
    ELSE
        IF TG_OP<>'INSERT' THEN
            pid:=OLD.period_id;
            IF EXISTS(SELECT 1 FROM examination_period WHERE period_id=pid AND status='PUBLISHED') THEN
                RAISE EXCEPTION 'Published period course and slot configuration is locked';
            END IF;
        END IF;
        IF TG_OP<>'DELETE' AND EXISTS(SELECT 1 FROM examination_period WHERE period_id=NEW.period_id AND status='PUBLISHED') THEN
            RAISE EXCEPTION 'Published period course and slot configuration is locked';
        END IF;
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER protect_published_period_setup BEFORE UPDATE OR DELETE ON public.examination_period
    FOR EACH ROW EXECUTE FUNCTION public.protect_published_period_setup();
CREATE TRIGGER protect_published_slot_setup BEFORE INSERT OR UPDATE OR DELETE ON public.examination_period_slot
    FOR EACH ROW EXECUTE FUNCTION public.protect_published_period_setup();
CREATE TRIGGER protect_published_course_setup BEFORE INSERT OR UPDATE OR DELETE ON public.examination_period_course
    FOR EACH ROW EXECUTE FUNCTION public.protect_published_period_setup();

CREATE FUNCTION public.protect_published_staffing() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP<>'INSERT' AND EXISTS(SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id)
        WHERE e.exam_session_id=OLD.exam_session_id AND p.status='PUBLISHED') THEN
        RAISE EXCEPTION 'Published staffing is locked';
    END IF;
    IF TG_OP<>'DELETE' AND EXISTS(SELECT 1 FROM exam_session e JOIN examination_period p USING(period_id)
        WHERE e.exam_session_id=NEW.exam_session_id AND p.status='PUBLISHED') THEN
        RAISE EXCEPTION 'Published staffing is locked';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER protect_published_staffing BEFORE INSERT OR UPDATE OR DELETE ON public.invigilator_assignment
    FOR EACH ROW EXECUTE FUNCTION public.protect_published_staffing();

ALTER TABLE public.scheduling_audit ADD COLUMN actor_subject text;
-- Lock lead-permission changes in the same order as an amendment's approval check.
CREATE TRIGGER schedule_write_lock BEFORE INSERT OR UPDATE OR DELETE ON public.scheduling_lead_permission
    FOR EACH STATEMENT EXECUTE FUNCTION public.lock_exam_schedule_writes();

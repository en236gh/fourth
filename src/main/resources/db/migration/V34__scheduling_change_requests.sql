-- Apply once after V33, in one transaction. Requests never change a timetable.
CREATE TABLE public.examination_change_request (
    request_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    requester_staff_id integer NOT NULL REFERENCES public.staff(staff_id),
    period_id integer NOT NULL REFERENCES public.examination_period(period_id),
    course_code varchar(15) NOT NULL REFERENCES public.course(course_code),
    exam_session_id integer REFERENCES public.exam_session(exam_session_id) ON DELETE SET NULL,
    proposed_change varchar(4000) NOT NULL CHECK (length(trim(proposed_change))>0),
    reason varchar(2000) NOT NULL CHECK (length(trim(reason))>0),
    status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_at timestamptz,
    decided_by_staff_id integer REFERENCES public.staff(staff_id),
    decision varchar(2000)
);
CREATE INDEX examination_change_request_period_idx ON public.examination_change_request(period_id,request_id);
CREATE TRIGGER scheduling_audit AFTER INSERT OR UPDATE OR DELETE ON public.examination_change_request
    FOR EACH ROW EXECUTE FUNCTION public.audit_scheduling_change();

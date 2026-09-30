-- Apply once after V37. Legacy coordinator and lead records remain for audit/history,
-- but no longer grant or restrict scheduling authority.
CREATE OR REPLACE FUNCTION public.approved_amendment_for(target integer) RETURNS bigint LANGUAGE sql STABLE AS $$
    SELECT a.amendment_id FROM examination_amendment a JOIN examination_period p USING(period_id)
    JOIN exam_session e ON e.exam_session_id=a.exam_session_id AND e.period_id=p.period_id
    JOIN staff reviewer ON reviewer.staff_id=a.reviewed_by_staff_id AND reviewer.account_status='ACTIVE'
    WHERE a.amendment_id=NULLIF(current_setting('app.scheduling_amendment',true),'')::bigint
    AND a.exam_session_id=target AND a.status='APPROVED' AND p.status='PUBLISHED' AND p.revision=a.expected_revision
    AND EXISTS(
        SELECT 1 FROM staff actor
        JOIN staff_role sr ON sr.staff_id=actor.staff_id
        JOIN role r ON r.role_id=sr.role_id AND r.name='ADMINISTRATOR'
        WHERE actor.staff_id=NULLIF(current_setting('app.scheduling_actor',true),'')::integer
        AND actor.account_status='ACTIVE'
    )
    AND e.status='SCHEDULED' AND e.exam_date+e.start_time>CURRENT_TIMESTAMP AT TIME ZONE p.timezone
    AND a.exam_date+a.start_time>CURRENT_TIMESTAMP AT TIME ZONE p.timezone
    AND EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=reviewer.staff_id AND r.name='ADMINISTRATOR')
    AND NOT EXISTS(SELECT 1 FROM attendance WHERE exam_session_id=target)
    AND NOT EXISTS(SELECT 1 FROM incident WHERE exam_session_id=target)
    AND NOT EXISTS(SELECT 1 FROM generated_report WHERE exam_session_id=target)
$$;

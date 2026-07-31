-- Clear demo exam attendance/incidents again so Postman check-in can be retested after the register JSON fix.

DELETE FROM public.attendance
WHERE exam_session_id IN (
    SELECT exam_session_id
    FROM public.exam_session
    WHERE course_code IN ('CS101', 'EE221', 'ED201')
);

DELETE FROM public.incident
WHERE exam_session_id IN (
    SELECT exam_session_id
    FROM public.exam_session
    WHERE course_code IN ('CS101', 'EE221', 'ED201')
);

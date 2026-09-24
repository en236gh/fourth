-- READ ONLY. One row per lecturer/course/exam; missing exams remain visible.
-- Counts use separate subqueries so shared majors do not multiply students/seats.
SELECT st.email, cl.course_code, es.exam_session_id, es.academic_year, es.semester,
       es.exam_date, registrations.total AS registered_students,
       venues.total AS venue_count, venues.capacity AS total_capacity,
       mappings.total AS active_curriculum_links,
       allocated.total AS existing_allocations,
       CASE
           WHEN st.account_status <> 'ACTIVE' THEN 'INACTIVE_ACCOUNT'
           WHEN NOT EXISTS (SELECT 1 FROM public.staff_role sr JOIN public.role r USING (role_id)
                            WHERE sr.staff_id = st.staff_id AND r.name = 'LECTURER') THEN 'MISSING_LECTURER_ROLE'
           WHEN es.exam_session_id IS NULL THEN 'NO_EXAM'
           WHEN mappings.total = 0 THEN 'NO_ACTIVE_EXAM_CURRICULUM_LINK'
           WHEN registrations.total = 0 THEN 'NO_REGISTERED_STUDENTS'
           WHEN venues.total = 0 THEN 'NO_EXAM_VENUES'
           WHEN registrations.total > venues.capacity THEN 'INSUFFICIENT_CAPACITY'
           ELSE 'READY_FOR_ALLOCATION'
       END AS setup_status
FROM public.course_lecturer cl
JOIN public.staff st USING (staff_id)
LEFT JOIN public.exam_session es ON es.course_code = cl.course_code
CROSS JOIN LATERAL (
    SELECT count(*) AS total FROM public.student_registration sr
    WHERE sr.course_code = es.course_code AND sr.academic_year = es.academic_year AND sr.semester = es.semester
) registrations
CROSS JOIN LATERAL (
    SELECT count(*) AS total, coalesce(sum(v.capacity), 0) AS capacity
    FROM public.exam_venue ev JOIN public.venue v USING (venue_id)
    WHERE ev.exam_session_id = es.exam_session_id
) venues
CROSS JOIN LATERAL (
    SELECT count(*) AS total FROM public.exam_session_programme_course epc
    JOIN public.programme_course pc USING (programme_course_id)
    JOIN public.programme p USING (programme_id)
    JOIN public.school s USING (school_id)
    JOIN public.course c ON c.course_code = pc.course_code
    LEFT JOIN public.major m ON m.major_id = pc.major_id AND m.programme_id = p.programme_id
    WHERE epc.exam_session_id = es.exam_session_id
      AND pc.course_code = es.course_code AND pc.semester = es.semester
      AND pc.is_active AND p.is_active AND s.is_active AND c.is_active
      AND (pc.major_id IS NULL OR m.is_active)
) mappings
CROSS JOIN LATERAL (
    SELECT count(*) AS total FROM public.student_venue_allocation sva
    WHERE sva.exam_session_id = es.exam_session_id
) allocated
ORDER BY st.email, cl.course_code, es.exam_date, es.exam_session_id;

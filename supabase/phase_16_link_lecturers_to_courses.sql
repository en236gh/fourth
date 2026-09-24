-- Run AFTER V30. Uses the existing five demo lecturer accounts and courses.
-- If the four extra accounts are missing, run V28 first. Does not reset passwords.
-- Supersedes V29 for this post-major setup; already-existing links are retained.
-- This single block validates and inserts all five links atomically.
-- No temporary table is needed; any failed check rolls back all inserts.
DO $$
DECLARE assignment record;
BEGIN
  FOR assignment IN
    SELECT * FROM (VALUES
        ('lecturer@gmail.com', 'CSC1101'),
        ('lecturer2@gmail.com', 'CSC1202'),
        ('lecturer3@gmail.com', 'CSC2201'),
        ('lecturer4@gmail.com', 'CSC2302'),
        ('lecturer5@gmail.com', 'CSC3101')
    ) AS assignments(email, course_code)
  LOOP
    IF NOT EXISTS (
        SELECT 1 FROM public.staff st
        JOIN public.staff_role sr ON sr.staff_id = st.staff_id
        JOIN public.role r ON r.role_id = sr.role_id
        WHERE st.email = assignment.email AND st.account_status = 'ACTIVE' AND r.name = 'LECTURER'
    ) THEN
        RAISE EXCEPTION 'Missing active lecturer account/role: %. Check account before retrying.', assignment.email;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.course c
        JOIN public.programme_course pc ON pc.course_code = c.course_code
        JOIN public.programme p ON p.programme_id = pc.programme_id
        JOIN public.school s ON s.school_id = p.school_id
        JOIN public.major m ON m.major_id = pc.major_id AND m.programme_id = p.programme_id
        WHERE c.course_code = assignment.course_code AND p.programme_code = 'BCS'
          AND s.school_code = 'SNS'
          AND c.is_active AND pc.is_active AND p.is_active AND s.is_active AND m.is_active
    ) THEN
        RAISE EXCEPTION 'Course missing active SNS -> BCS -> major path: %. Complete V30 first.', assignment.course_code;
    END IF;

    INSERT INTO public.course_lecturer (course_code, staff_id)
    SELECT assignment.course_code, st.staff_id
    FROM public.staff st WHERE st.email = assignment.email
    ON CONFLICT (course_code, staff_id) DO NOTHING;
  END LOOP;
END $$;

-- Each lecturer may appear several times because courses are shared across majors.
SELECT st.email, s.school_name, p.programme_name, m.major_name,
       pc.year_of_study, pc.semester, cl.course_code
FROM public.course_lecturer cl
JOIN public.staff st USING (staff_id)
JOIN public.programme_course pc USING (course_code)
JOIN public.programme p USING (programme_id)
JOIN public.school s USING (school_id)
JOIN public.major m ON m.major_id = pc.major_id AND m.programme_id = p.programme_id
WHERE st.email IN ('lecturer@gmail.com', 'lecturer2@gmail.com', 'lecturer3@gmail.com',
                   'lecturer4@gmail.com', 'lecturer5@gmail.com')
  AND s.is_active AND p.is_active AND m.is_active AND pc.is_active
ORDER BY st.email, cl.course_code, m.major_name, pc.year_of_study, pc.semester;

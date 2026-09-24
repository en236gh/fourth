-- READ ONLY. Run after V30__programme_majors.sql has committed successfully.
-- Expect six rows: three majors under BCS and three under BENG.
SELECT s.school_code, p.programme_code, p.programme_name,
       m.major_code, m.major_name, count(pc.programme_course_id) AS curriculum_rows
FROM public.major m
JOIN public.programme p USING (programme_id)
JOIN public.school s USING (school_id)
LEFT JOIN public.programme_course pc
  ON pc.major_id = m.major_id AND pc.programme_id = m.programme_id
WHERE p.programme_code IN ('BCS', 'BENG')
GROUP BY s.school_code, p.programme_code, p.programme_name, m.major_code, m.major_name
ORDER BY p.programme_code, m.major_code;

-- Both counts must be zero.
SELECT count(*) AS curriculum_rows_still_on_legacy_programmes
FROM public.programme_course pc
JOIN public.major m ON m.legacy_programme_id = pc.programme_id;
SELECT count(*) AS legacy_programmes_still_active
FROM public.programme p JOIN public.major m ON m.legacy_programme_id = p.programme_id
WHERE p.is_active;

-- Counts and curriculum IDs must match preflight results.
SELECT count(*) AS curriculum_rows,
       array_agg(programme_course_id ORDER BY programme_course_id) AS curriculum_ids
FROM public.programme_course;
SELECT count(*) AS exam_curriculum_links FROM public.exam_session_programme_course;

-- Active programme selector: BCS, BENG and the unchanged Education entries.
SELECT s.school_code, p.programme_code, p.programme_name
FROM public.programme p JOIN public.school s USING (school_id)
WHERE p.is_active AND s.is_active ORDER BY s.school_code, p.programme_code;

-- Lecturer -> course -> programme -> major. Empty results mean lecturer links are missing.
SELECT st.email, cl.course_code, p.programme_code, m.major_code,
       pc.year_of_study, pc.semester
FROM public.course_lecturer cl
JOIN public.staff st USING (staff_id)
JOIN public.programme_course pc USING (course_code)
JOIN public.programme p USING (programme_id)
LEFT JOIN public.major m ON m.major_id = pc.major_id
WHERE p.is_active AND pc.is_active
ORDER BY st.email, cl.course_code, p.programme_code, m.major_code;

-- Optional: run separately only if student_programme_enrolment exists.
-- SELECT count(*) AS enrolments_still_on_legacy_programmes
-- FROM public.student_programme_enrolment e
-- JOIN public.major m ON m.legacy_programme_id = e.programme_id;
-- Expected: zero.

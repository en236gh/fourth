-- READ ONLY. Run before V30__programme_majors.sql against your existing database.
SELECT current_setting('server_version') AS postgres_version;

-- Required tables must all exist. Enrolments are optional: V30 migrates them if present.
SELECT name, to_regclass('public.' || name) AS existing_table
FROM (VALUES ('school'), ('programme'), ('course'), ('programme_course'),
             ('exam_session_programme_course'), ('student_programme_enrolment')) t(name);

-- Expect all six rows to have an existing programme and the expected school.
SELECT expected.code, expected.school_code AS expected_school,
       p.programme_id, s.school_code AS actual_school, p.is_active
FROM (VALUES ('BCS-CS', 'SNS'), ('BCS-SE', 'SNS'), ('BCS-NIS', 'SNS'),
             ('BENG-EE', 'ENG'), ('BENG-CE', 'ENG'), ('BENG-ME', 'ENG')) expected(code, school_code)
LEFT JOIN public.programme p ON p.programme_code = expected.code
LEFT JOIN public.school s ON s.school_id = p.school_id
ORDER BY expected.code;

-- Save these counts and ID lists to compare with the post-migration results.
SELECT count(*) AS curriculum_rows,
       array_agg(programme_course_id ORDER BY programme_course_id) AS curriculum_ids
FROM public.programme_course;
SELECT count(*) AS exam_curriculum_links FROM public.exam_session_programme_course;

-- Inspect existing parents, if any. Expected schools: BCS -> SNS; BENG -> ENG.
SELECT p.programme_code, s.school_code, p.is_active
FROM public.programme p JOIN public.school s USING (school_id)
WHERE p.programme_code IN ('BCS', 'BENG');

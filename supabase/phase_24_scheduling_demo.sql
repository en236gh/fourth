-- DEVELOPMENT DATABASE ONLY. Run V32 first. No existing records are modified.
-- Explicit opt-in in the same SQL session:
-- SET app.allow_demo_seed = 'true';
-- Optional Monday date, set before first run: SET app.demo_start_date = '2030-01-07';
-- Default: first Monday in January two years from now. Reruns reuse the original date.
BEGIN;
DO $$
DECLARE
    start_day DATE;
    demo_year TEXT;
    sch INTEGER;
    prog INTEGER;
    room1 INTEGER;
    room2 INTEGER;
    external_exam INTEGER;
    lecturer INTEGER;
BEGIN
    IF current_setting('app.allow_demo_seed',true) IS DISTINCT FROM 'true' THEN
        RAISE EXCEPTION 'Development seed requires SET app.allow_demo_seed = true in an isolated development database';
    END IF;
    SELECT exam_date INTO start_day FROM exam_session WHERE course_code='DEMOEXT' ORDER BY exam_session_id LIMIT 1;
    IF start_day IS NULL THEN
        start_day := NULLIF(current_setting('app.demo_start_date',true),'')::date;
        IF start_day IS NULL THEN
            start_day := make_date(extract(year from current_date)::integer+2,1,1);
            start_day := start_day + ((8-extract(isodow from start_day)::integer)%7);
        END IF;
    END IF;
    IF extract(isodow from start_day)<>1 THEN RAISE EXCEPTION 'Demo start date must be a Monday'; END IF;
    demo_year := extract(year from start_day)::integer || '/' || (extract(year from start_day)::integer+1);

    INSERT INTO school(school_code,school_name) VALUES ('DEMO-SCHED','Scheduling Demonstration School') ON CONFLICT DO NOTHING;
    SELECT school_id INTO sch FROM school WHERE school_code='DEMO-SCHED';
    INSERT INTO programme(school_id,programme_code,programme_name,duration_years)
        VALUES (sch,'DEMO-SCHED','Scheduling Demonstration Programme',4) ON CONFLICT DO NOTHING;
    SELECT programme_id INTO prog FROM programme WHERE programme_code='DEMO-SCHED';
    INSERT INTO course(course_code,course_name) VALUES
        ('DEMO101','Demo: one registered student'),('DEMO102','Demo: unrelated students'),
        ('DEMO201','Demo: shared student'),('DEMO301','Demo: multiple venues'),
        ('DEMO999','Demo: insufficient capacity'),('DEMOEXT','Demo: existing reservation') ON CONFLICT DO NOTHING;
    INSERT INTO programme_course(programme_id,course_code,year_of_study,semester)
        SELECT prog,course_code,1,1 FROM course c WHERE course_code IN ('DEMO101','DEMO102','DEMO201','DEMO301','DEMO999','DEMOEXT')
        AND NOT EXISTS(SELECT 1 FROM programme_course pc WHERE pc.programme_id=prog AND pc.course_code=c.course_code AND pc.semester=1);

    INSERT INTO student(computer_number,national_id,full_name,program,school,year_of_study,photo_path,qr_token,status)
        SELECT 'SD'||lpad(n::text,6,'0'),'SD-NRC-'||n,'Scheduling Demo Student '||n,
        'Scheduling Demonstration Programme','Scheduling Demonstration School',1,'/images/demo-placeholder.png','sched-demo-'||n,'ACTIVE'
        FROM generate_series(1,20) n WHERE NOT EXISTS(SELECT 1 FROM student WHERE computer_number='SD'||lpad(n::text,6,'0'));
    INSERT INTO student_programme_enrolment(computer_number,programme_id,academic_year,year_of_study,enrolment_status)
        SELECT computer_number,prog,demo_year,1,'ACTIVE' FROM student s WHERE computer_number LIKE 'SD0000%'
        AND NOT EXISTS(SELECT 1 FROM student_programme_enrolment e WHERE e.computer_number=s.computer_number AND e.academic_year=demo_year);
    INSERT INTO student_registration(computer_number,course_code,academic_year,semester)
        SELECT x.student,x.course,demo_year,1 FROM (VALUES
            ('SD000001','DEMO101'),('SD000002','DEMO102'),('SD000003','DEMO102'),
            ('SD000001','DEMO201'),('SD000004','DEMO201'),
            ('SD000001','DEMO301'),('SD000002','DEMO301'),('SD000003','DEMO301'),('SD000004','DEMO301'),('SD000005','DEMO301'),
            ('SD000006','DEMOEXT'),('SD000007','DEMOEXT'),('SD000008','DEMOEXT')) x(student,course)
        WHERE NOT EXISTS(SELECT 1 FROM student_registration r WHERE r.computer_number=x.student AND r.course_code=x.course AND r.academic_year=demo_year AND r.semester=1);
    INSERT INTO student_registration(computer_number,course_code,academic_year,semester)
        SELECT computer_number,'DEMO999',demo_year,1 FROM student s WHERE computer_number LIKE 'SD0000%'
        AND NOT EXISTS(SELECT 1 FROM student_registration r WHERE r.computer_number=s.computer_number AND r.course_code='DEMO999' AND r.academic_year=demo_year AND r.semester=1);

    INSERT INTO venue(venue_name,building,capacity,examination_capacity)
        SELECT 'Scheduling Demo Room 1','Demo',30,3 WHERE NOT EXISTS(SELECT 1 FROM venue WHERE venue_name='Scheduling Demo Room 1');
    INSERT INTO venue(venue_name,building,capacity,examination_capacity)
        SELECT 'Scheduling Demo Room 2','Demo',30,3 WHERE NOT EXISTS(SELECT 1 FROM venue WHERE venue_name='Scheduling Demo Room 2');
    SELECT venue_id INTO room1 FROM venue WHERE venue_name='Scheduling Demo Room 1';
    SELECT venue_id INTO room2 FROM venue WHERE venue_name='Scheduling Demo Room 2';
    INSERT INTO venue_unavailability(venue_id,starts_at,ends_at,reason)
        SELECT room2,(start_day+1)+time '09:00',(start_day+1)+time '11:00','Scheduling demo maintenance'
        WHERE NOT EXISTS(SELECT 1 FROM venue_unavailability WHERE venue_id=room2 AND reason='Scheduling demo maintenance');

    INSERT INTO exam_session(course_code,exam_date,start_time,end_time,academic_year,semester,exam_type,status)
        SELECT 'DEMOEXT',start_day,'09:00','11:00',demo_year,1,'FINAL','SCHEDULED'
        WHERE NOT EXISTS(SELECT 1 FROM exam_session WHERE course_code='DEMOEXT' AND academic_year=demo_year);
    SELECT exam_session_id INTO external_exam FROM exam_session WHERE course_code='DEMOEXT' AND academic_year=demo_year LIMIT 1;
    INSERT INTO exam_venue SELECT external_exam,room1 WHERE NOT EXISTS(SELECT 1 FROM exam_venue WHERE exam_session_id=external_exam AND venue_id=room1);
    INSERT INTO student_venue_allocation(computer_number,exam_session_id,venue_id)
        SELECT r.computer_number,external_exam,room1 FROM student_registration r WHERE r.course_code='DEMOEXT' AND r.academic_year=demo_year
        AND NOT EXISTS(SELECT 1 FROM student_venue_allocation a WHERE a.exam_session_id=external_exam AND a.computer_number=r.computer_number);

    INSERT INTO staff(full_name,email,department,account_status)
        VALUES ('Scheduling Demo Lecturer','scheduling-lecturer@example.invalid','Demo','ACTIVE'),
               ('Scheduling Demo Invigilator 1','scheduling-invigilator1@example.invalid','Demo','ACTIVE'),
               ('Scheduling Demo Invigilator 2','scheduling-invigilator2@example.invalid','Demo','ACTIVE') ON CONFLICT DO NOTHING;
    INSERT INTO staff_role(staff_id,role_id)
        SELECT s.staff_id,r.role_id FROM staff s CROSS JOIN role r
        WHERE (s.email='scheduling-lecturer@example.invalid' AND r.name='LECTURER')
           OR (s.email IN ('scheduling-invigilator1@example.invalid','scheduling-invigilator2@example.invalid') AND r.name='INVIGILATOR')
        ON CONFLICT DO NOTHING;
    SELECT staff_id INTO lecturer FROM staff WHERE email='scheduling-lecturer@example.invalid';
    INSERT INTO course_lecturer(course_code,staff_id) VALUES ('DEMO101',lecturer) ON CONFLICT DO NOTHING;
    INSERT INTO invigilator_assignment(exam_session_id,venue_id,staff_id,assignment_status,published_at)
        SELECT external_exam,room1,staff_id,'PUBLISHED',CURRENT_TIMESTAMP FROM staff WHERE email='scheduling-invigilator1@example.invalid'
        AND NOT EXISTS(SELECT 1 FROM invigilator_assignment WHERE exam_session_id=external_exam AND venue_id=room1);
    RAISE NOTICE 'Demo start %, end %, academic year %. Select DEMO101, DEMO102, DEMO201, DEMO301; add DEMO999 to demonstrate insufficient capacity.',start_day,start_day+4,demo_year;
END $$;
COMMIT;

-- Multiple draft periods may share an academic year, semester and exam type.
-- Published cycle conflicts are still checked during timetable generation.
ALTER TABLE public.examination_period
    DROP CONSTRAINT IF EXISTS examination_period_academic_year_semester_exam_type_key;
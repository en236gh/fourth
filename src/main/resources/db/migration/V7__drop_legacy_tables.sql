-- Remove legacy and irrelevant tables from the attendance-focused schema.

DROP TABLE IF EXISTS public.course_lecturer CASCADE;
DROP TABLE IF EXISTS public.course CASCADE;
DROP TABLE IF EXISTS public.exam_venue CASCADE;
DROP TABLE IF EXISTS public.invigilator_assignment CASCADE;
DROP TABLE IF EXISTS public.student_registration CASCADE;
DROP TABLE IF EXISTS public.incidence CASCADE;

-- Reset all invigilator and student activity history back to zero while preserving the accounts themselves.
-- Keep: staff, students, roles, venues, exam sessions, allocations, and assignments.
-- Clear: exam activity, reports, login tokens, and any pass history tied to current accounts.

DELETE FROM public.attendance;
DELETE FROM public.incident;
DELETE FROM public.generated_report;
DELETE FROM public.examination_pass;
DELETE FROM public.student_refresh_token;
DELETE FROM public.refresh_token;

UPDATE public.exam_session
SET status = 'SCHEDULED';

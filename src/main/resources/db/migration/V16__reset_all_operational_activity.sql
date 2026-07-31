-- Full operational wipe for a clean invigilator/student demo restart.
-- Keeps: students, staff, venues, exam sessions, allocations, invigilator assignments.
-- Clears: attendance, incidents, generated reports; resets exam status to SCHEDULED.

DELETE FROM public.attendance;
DELETE FROM public.incident;
DELETE FROM public.generated_report;

UPDATE public.exam_session
SET status = 'SCHEDULED';

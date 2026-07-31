-- Fix check-in failures: app writes attendance_status, but legacy status was still NOT NULL.
-- Also clear demo exam attendance so Postman check-in can be retested cleanly.

UPDATE public.attendance
SET attendance_status = COALESCE(attendance_status, status, 'PRESENT')
WHERE attendance_status IS NULL;

ALTER TABLE public.attendance
    ALTER COLUMN attendance_status SET DEFAULT 'PRESENT';

ALTER TABLE public.attendance
    ALTER COLUMN attendance_status SET NOT NULL;

-- Drop legacy status column and its constraints/indexes.
ALTER TABLE public.attendance
    DROP CONSTRAINT IF EXISTS attendance_record_status_check;

DROP INDEX IF EXISTS public.idx_attendance_status;

ALTER TABLE public.attendance
    DROP COLUMN IF EXISTS status;

-- Recreate status index on the canonical column.
CREATE INDEX IF NOT EXISTS idx_attendance_status
    ON public.attendance (attendance_status);

-- Reset demo examination attendance (CS101 / EE221 / ED201) for clean Postman testing.
DELETE FROM public.attendance
WHERE exam_session_id IN (
    SELECT exam_session_id
    FROM public.exam_session
    WHERE course_code IN ('CS101', 'EE221', 'ED201')
);

-- Optional: remove demo incidents for those same exams so incident reporting can be retested.
DELETE FROM public.incident
WHERE exam_session_id IN (
    SELECT exam_session_id
    FROM public.exam_session
    WHERE course_code IN ('CS101', 'EE221', 'ED201')
);

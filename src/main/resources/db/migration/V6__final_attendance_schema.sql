-- Align the schema around the core examination attendance entities.

ALTER TABLE IF EXISTS public.attendance_record RENAME TO attendance;
ALTER TABLE IF EXISTS public.incident_report RENAME TO incident;

CREATE TABLE IF NOT EXISTS public.role (
    role_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO public.role (name)
SELECT DISTINCT role
FROM public.staff_role
WHERE role IS NOT NULL
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.staff_role_new (
    staff_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    PRIMARY KEY (staff_id, role_id),
    CONSTRAINT fk_staff_role_staff FOREIGN KEY (staff_id) REFERENCES public.staff(staff_id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_role_role FOREIGN KEY (role_id) REFERENCES public.role(role_id) ON DELETE CASCADE
);

INSERT INTO public.staff_role_new (staff_id, role_id)
SELECT sr.staff_id, r.role_id
FROM public.staff_role sr
JOIN public.role r ON r.name = sr.role
ON CONFLICT DO NOTHING;

DROP TABLE IF EXISTS public.staff_role;
ALTER TABLE public.staff_role_new RENAME TO staff_role;

ALTER TABLE public.attendance
    DROP CONSTRAINT IF EXISTS attendance_record_verification_method_check;

ALTER TABLE public.attendance
    ADD CONSTRAINT attendance_verification_method_check
    CHECK (verification_method IN ('COMPUTER','QR_CODE','FACIAL_RECOGNITION','QR_AND_FACE','QR_AND_FACIAL'));

ALTER TABLE public.attendance
    ADD COLUMN IF NOT EXISTS attendance_status VARCHAR(30);

UPDATE public.attendance
SET attendance_status = COALESCE(status, 'PRESENT')
WHERE attendance_status IS NULL;

ALTER TABLE public.attendance
    ALTER COLUMN attendance_status SET DEFAULT 'PRESENT';

ALTER TABLE public.attendance
    ADD CONSTRAINT attendance_status_check
    CHECK (attendance_status IN ('PRESENT','ABSENT','LATE','WRONG_VENUE'));

CREATE INDEX IF NOT EXISTS idx_attendance_exam_session ON public.attendance (exam_session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_status ON public.attendance (attendance_status);

ALTER TABLE public.incident
    ADD COLUMN IF NOT EXISTS incident_type VARCHAR(50);
ALTER TABLE public.incident
    ADD COLUMN IF NOT EXISTS evidence_path VARCHAR(500);
ALTER TABLE public.incident
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE public.incident
SET incident_type = COALESCE(incident_type, 'OTHER'),
    occurred_at = COALESCE(occurred_at, incident_time, CURRENT_TIMESTAMP)
WHERE incident_type IS NULL OR occurred_at IS NULL;

ALTER TABLE public.incident
    ALTER COLUMN occurred_at SET NOT NULL;
ALTER TABLE public.incident
    ALTER COLUMN description SET NOT NULL;
ALTER TABLE public.incident
    ALTER COLUMN severity SET DEFAULT 'MINOR';

ALTER TABLE public.generated_report
    ADD COLUMN IF NOT EXISTS generated_by_staff_id INTEGER;
ALTER TABLE public.generated_report
    ADD CONSTRAINT fk_generated_report_staff
    FOREIGN KEY (generated_by_staff_id) REFERENCES public.staff(staff_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_generated_report_exam_session ON public.generated_report(exam_session_id);

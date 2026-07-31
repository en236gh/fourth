ALTER TABLE public.attendance_record
    DROP CONSTRAINT IF EXISTS attendance_record_verification_method_check;

ALTER TABLE public.attendance_record
    ADD CONSTRAINT attendance_record_verification_method_check
    CHECK (
        verification_method IN ('COMPUTER','QR_CODE','FACIAL_RECOGNITION','QR_AND_FACE','QR_AND_FACIAL')
    );

CREATE TABLE IF NOT EXISTS public.role (
    role_id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO public.role (name)
VALUES ('ADMINISTRATOR'), ('LECTURER'), ('INVIGILATOR')
ON CONFLICT (name) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.refresh_token (
    token_id BIGSERIAL PRIMARY KEY,
    staff_id INTEGER NOT NULL REFERENCES public.staff(staff_id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_staff ON public.refresh_token(staff_id);

CREATE TABLE IF NOT EXISTS public.generated_report (
    report_id BIGSERIAL PRIMARY KEY,
    exam_session_id INTEGER NOT NULL REFERENCES public.exam_session(exam_session_id) ON DELETE CASCADE,
    generated_by_staff_id INTEGER REFERENCES public.staff(staff_id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    report_type VARCHAR(50) NOT NULL DEFAULT 'EXAMINATION_ATTENDANCE',
    file_path VARCHAR(500) NOT NULL,
    generated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary JSONB
);

CREATE INDEX IF NOT EXISTS idx_generated_report_session ON public.generated_report(exam_session_id);

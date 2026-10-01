-- Apply once after V38. QR + face verification at examination check-in.
-- Only face embeddings (512 numbers from the ArcFace model) are stored, never photos.

CREATE TABLE IF NOT EXISTS public.student_face_template (
    computer_number VARCHAR(15) PRIMARY KEY
        REFERENCES public.student (computer_number) ON DELETE CASCADE,
    embedding REAL[] NOT NULL CHECK (cardinality(embedding) = 512),
    model_name VARCHAR(50) NOT NULL,
    detection_score REAL NOT NULL,
    consent_recorded_at TIMESTAMP NOT NULL,
    enrolled_by_staff_id INTEGER REFERENCES public.staff (staff_id) ON DELETE SET NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.attendance
    ADD COLUMN IF NOT EXISTS face_match_score REAL,
    ADD COLUMN IF NOT EXISTS face_override_reason VARCHAR(500);

ALTER TABLE public.incident
    DROP CONSTRAINT IF EXISTS incident_type_check;

ALTER TABLE public.incident
    ADD CONSTRAINT incident_type_check
    CHECK (incident_type IN (
        'CHEATING',
        'PHONE_FOUND',
        'WRONG_VENUE',
        'MEDICAL_EMERGENCY',
        'DISTURBANCE',
        'LATE_ARRIVAL',
        'IMPERSONATION',
        'OTHER'
    ));

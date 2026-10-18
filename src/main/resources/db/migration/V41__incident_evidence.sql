INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('incident-evidence', 'incident-evidence', false, 10485760,
        ARRAY['image/jpeg', 'image/png', 'application/pdf'])
ON CONFLICT (id) DO UPDATE
SET public = false,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'application/pdf'];

CREATE TABLE IF NOT EXISTS public.incident_evidence (
    evidence_id SERIAL PRIMARY KEY,
    incident_id INTEGER NOT NULL REFERENCES public.incident(incident_id) ON DELETE CASCADE,
    storage_bucket VARCHAR(120) NOT NULL DEFAULT 'incident-evidence',
    object_path VARCHAR(1000) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png', 'application/pdf')),
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes BETWEEN 1 AND 10485760),
    uploaded_by_staff_id INTEGER NOT NULL REFERENCES public.staff(staff_id) ON DELETE RESTRICT,
    uploaded_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT incident_evidence_bucket_path_unique UNIQUE (storage_bucket, object_path)
);

CREATE INDEX IF NOT EXISTS idx_incident_evidence_incident
    ON public.incident_evidence (incident_id, uploaded_at);
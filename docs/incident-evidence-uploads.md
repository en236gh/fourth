# Incident Evidence Uploads

Uploaded evidence is stored in the private Supabase Storage bucket `incident-evidence`. The existing JSON `POST /api/incidents` endpoint is unchanged; `evidencePath` remains the legacy evidence link and is separate from uploaded attachments.

## Setup

Apply [phase_28_incident_evidence.sql](../supabase/phase_28_incident_evidence.sql) once in the target Supabase SQL editor. It creates or secures the private bucket and creates `public.incident_evidence`. The application has Flyway disabled, so restarting it will not apply this schema. `src/main/resources/db/migration/V41__incident_evidence.sql` is provided for installations that later enable Flyway.

Configure the backend environment with the Supabase project's S3-compatible endpoint and S3 credentials generated from Supabase Storage settings. Do not use the public `backend_storage` bucket for incident evidence, and do not commit these credentials:

```dotenv
SUPABASE_STORAGE_S3_ENDPOINT=https://<project-ref>.storage.supabase.co/storage/v1/s3
SUPABASE_STORAGE_S3_REGION=eu-west-2
SUPABASE_STORAGE_S3_ACCESS_KEY=<generated-s3-access-key>
SUPABASE_STORAGE_S3_SECRET_KEY=<generated-s3-secret-key>
INCIDENT_EVIDENCE_BUCKET=incident-evidence
```

The endpoint, region, credentials, and bucket have safe defaults or empty defaults in `application.properties`; storage operations fail closed until endpoint and credentials are configured. Rotate any credential that has been shared outside a secret manager or private deployment configuration.

## API

All routes require a bearer token. Invigilators may upload, list, or request a download URL only when they have a `PUBLISHED` assignment for the incident's exact examination and venue, and the examination schedule is published. Administrators may access evidence across incidents.

Upload one file per request. Multiple requests create multiple attachments:

```http
POST /api/incidents/123/evidence
Authorization: Bearer <access-token>
Content-Type: multipart/form-data

file=<JPEG, PNG, or PDF up to 10 MiB>
```

The server decodes JPEG/PNG images and parses PDFs, verifies the declared MIME type against the detected content, and enforces the size limit. The response contains metadata only, including the generated storage path; it does not include a signed URL.

```http
GET /api/incidents/123/evidence
GET /api/incidents/123/evidence/45/download-url
```

Download URL responses expire five minutes after generation. The signed URL is intentionally short-lived and should not be persisted by clients. Attachment metadata stores the bucket, object path, submitted filename, detected MIME type, byte size, uploader, and upload time. Phone-local paths and signed URLs are never stored.

If an upload fails, the incident remains unchanged and the same endpoint can be retried. If metadata persistence fails after the object upload, the backend attempts to delete the unlinked object.

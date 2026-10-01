# Facial verification (QR + face) — backend and frontend guide

The invigilator scans the student's examination-pass QR, which says **who the student claims to be**,
then takes a photo, which **confirms it**. This is 1:1 verification against one enrolled template; the
system never searches all students' faces.

```
Phone (invigilator)            Spring Boot :8080                        face-service :8000 (FastAPI)
───────────────────            ──────────────────                       ────────────────────────────
scan QR ── lookup-by-qr ─────► eligibility + venue + faceEnrolled
capture photo
POST check-in-by-qr-and-face ► re-check eligibility
                               load student_face_template
                               POST /embed (photo) ───────────────────► RetinaFace detect → ArcFace
                                                   ◄─────────────────── 512-d unit embedding
                               cosine(template, probe)
                               ≥ 0.45 VERIFIED → attendance (QR_AND_FACE, score)
                               0.30–0.45 REVIEW_REQUIRED (or CONFIRMED_BY_INVIGILATOR with reason)
                               < 0.30 REJECTED → IMPERSONATION incident
            ◄───────────────── outcome
```

Photos are never stored. Only the 512-number embedding is kept (`student_face_template`), with consent time
and the enrolling administrator. Deleting the enrolment removes it.

## Running it

1. Apply `src/main/resources/db/migration/V39__facial_verification.sql` in the Supabase SQL editor
   (Flyway is disabled in `application.properties`). **The backend must not be deployed before this runs**,
   because `Attendance` now maps `face_match_score` and `face_override_reason`.
2. Start the face service:
   - Docker: `docker compose up --build`. The first start downloads the `buffalo_l` weights (~280 MB) into
     the `insightface-models` volume.
   - Without Docker (Python 3.10–3.12 recommended):
     ```
     cd face-service
     python -m venv .venv && .venv\Scripts\activate
     pip install -r requirements-dev.txt
     uvicorn app.main:app --port 8000
     ```
3. Optional shared secret: set `FACE_SERVICE_API_KEY` in `.env`; both services read it.
4. Tune with `FACE_MATCH_THRESHOLD` (default 0.45) and `FACE_REVIEW_THRESHOLD` (default 0.30). Calibrate on your
   own photos: `FACE_LIVE_TEST=1 pytest tests/test_live_model.py -s` prints same/different-person scores.

The `buffalo_l` weights are licensed by InsightFace for **non-commercial research** only. That is fine for
this project; a production deployment would need a commercial licence or a permissively licensed model such as
OpenCV SFace.

## API

All responses use the usual `ApiResponse` envelope `{ success, message, data }`.

### Enrolment (administrator, in person)

| Method | Path | Body |
|---|---|---|
| `POST` | `/api/admin/students/{computerNumber}/face` | multipart: `image` (JPEG/PNG ≤ 5 MB), `consentConfirmed=true` |
| `GET` | `/api/admin/students/{computerNumber}/face` | — |
| `DELETE` | `/api/admin/students/{computerNumber}/face` | — (consent withdrawn) |

`data`: `{ computerNumber, enrolled, modelName, detectionScore, enrolledAt, updatedAt }`.
Re-posting replaces the template. Students cannot enrol themselves: someone sitting an exam for a friend could
otherwise enrol their own face on the friend's account.

### Check-in (invigilator)

`GET /api/attendance/lookup` and `POST /api/attendance/lookup-by-qr` now also return `faceEnrolled: boolean`.

`POST /api/attendance/check-in-by-qr-and-face`, multipart:

| Field | Required | Notes |
|---|---|---|
| `qrToken` | yes | the scanned pass token |
| `examSessionId`, `venueId` | yes | as for `check-in-by-qr` |
| `image` | yes | JPEG from the camera, ≤ 5 MB |
| `overrideReason` | no | ≤ 300 chars; only used in the review band |

`data`:

```json
{
  "outcome": "VERIFIED | CONFIRMED_BY_INVIGILATOR | REVIEW_REQUIRED | REJECTED | NOT_ENROLLED",
  "computerNumber": "2022004264",
  "similarity": 0.6123,
  "matchThreshold": 0.45,
  "reviewThreshold": 0.3,
  "attendance": { "...": "AttendanceCheckInResponse, now with faceMatchScore; null unless recorded" },
  "incidentId": null,
  "message": "Face verified. Attendance recorded."
}
```

Every decision is HTTP 200 with an `outcome`. Errors:

| Status | When | UI action |
|---|---|---|
| 400 / 409 | the existing QR, eligibility, venue or duplicate rules | same as `check-in-by-qr` |
| 409 | template enrolled with a different model | ask an administrator to re-enrol |
| 413 | photo > 5 MB | compress and retry (see capture code) |
| 422 | no face, several faces, face too small, unreadable image | show `message`, retake |
| 503 | face service down | fall back to `check-in-by-qr` |

`/api/attendance/check-in` no longer accepts face verification methods (`QR_AND_FACE`, `FACE_RECOGNITION`,
`QR_AND_FACIAL`). A client cannot claim a face check the server did not perform.

## Frontend

### Invigilator check-in screen

```
[Scan QR] → lookup-by-qr → student card (name, programme, venue, faceEnrolled badge)
   │
   ├─ faceEnrolled = false → [Check in with QR] (existing check-in-by-qr) + "Not enrolled for face" note
   └─ faceEnrolled = true  → camera view with oval guide → [Capture] → POST check-in-by-qr-and-face
                               VERIFIED                 → green tick, score, next student
                               REVIEW_REQUIRED          → amber panel: [Retake] or reason box + [Confirm identity]
                               CONFIRMED_BY_INVIGILATOR → green tick with "manual" tag
                               REJECTED                 → red panel: "Do not admit, check ID", incident #id link
                               NOT_ENROLLED             → offer [Check in with QR]
                               422                      → toast with message, stay on camera
                               503                      → banner "Face check unavailable", [Check in with QR]
```

Keep the camera stream open between students so each capture is instant. In the amber state, show the score and
let the invigilator retake up to two or three times before typing a reason.

### Camera capture (browser, React example)

`getUserMedia` only works on HTTPS or `localhost`. When testing from a phone on the LAN, serve the frontend over
HTTPS (for example with `vite --https` or `mkcert`) and add that origin to `APP_CORS_ALLOWED_ORIGINS`.

```tsx
import { useEffect, useRef, useState } from "react";

export function useCamera() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let stream: MediaStream | undefined;
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: "environment", width: { ideal: 1280 }, height: { ideal: 720 } }, audio: false })
      .then((s) => { stream = s; if (videoRef.current) videoRef.current.srcObject = s; })
      .catch(() => setError("Camera permission is required for face verification."));
    return () => stream?.getTracks().forEach((t) => t.stop());
  }, []);

  /** Grabs the current frame as a JPEG, downscaled so uploads stay small (~100–200 KB). */
  async function capture(maxSide = 960): Promise<Blob> {
    const video = videoRef.current!;
    const scale = Math.min(1, maxSide / Math.max(video.videoWidth, video.videoHeight));
    const canvas = document.createElement("canvas");
    canvas.width = Math.round(video.videoWidth * scale);
    canvas.height = Math.round(video.videoHeight * scale);
    canvas.getContext("2d")!.drawImage(video, 0, 0, canvas.width, canvas.height);
    return new Promise((resolve) => canvas.toBlob((b) => resolve(b!), "image/jpeg", 0.85));
  }

  return { videoRef, capture, error };
}
```

```tsx
type Outcome = "VERIFIED" | "CONFIRMED_BY_INVIGILATOR" | "REVIEW_REQUIRED" | "REJECTED" | "NOT_ENROLLED";

export async function checkInWithFace(args: {
  token: string; qrToken: string; examSessionId: number; venueId: number; photo: Blob; overrideReason?: string;
}) {
  const form = new FormData();
  form.append("qrToken", args.qrToken);
  form.append("examSessionId", String(args.examSessionId));
  form.append("venueId", String(args.venueId));
  form.append("image", args.photo, "capture.jpg");
  if (args.overrideReason) form.append("overrideReason", args.overrideReason);

  // Do not set Content-Type yourself: the browser adds the multipart boundary.
  const res = await fetch(`${API_URL}/api/attendance/check-in-by-qr-and-face`, {
    method: "POST",
    headers: { Authorization: `Bearer ${args.token}` },
    body: form,
  });
  const body = await res.json();
  if (res.status === 503) return { kind: "fallback" as const, message: body.message };
  if (res.status === 422) return { kind: "retake" as const, message: body.message };
  if (!res.ok) throw new Error(body.message);
  return { kind: "decision" as const, outcome: body.data.outcome as Outcome, data: body.data };
}
```

A front-facing camera (`facingMode: "user"`) suits a self-service kiosk. A handheld phone pointed at the student
should use `"environment"`.

### Administrator enrolment screen

Add it to the student page: a live camera preview (or file upload of a passport-style photo), a required
**"Student has given consent to biometric verification"** checkbox, then `POST /api/admin/students/{cn}/face` with
`image` and `consentConfirmed=true`. Show the enrolment status from `GET` and a "Remove face data" button that
calls `DELETE`. Enrol under even, front-facing light. The enrolment photo sets the quality ceiling for every later
check-in.

### Reports and dashboards

`AttendanceCheckInResponse.faceMatchScore` is now available in attendance lists. Useful views:
- a "Manual confirmations" filter: `verificationMethod = QR_AND_FACE` with an `alertMessage` that starts with
  "Face similarity", for lecturer or admin review
- `IMPERSONATION` incidents in the incidents list, already returned by `/api/incidents`

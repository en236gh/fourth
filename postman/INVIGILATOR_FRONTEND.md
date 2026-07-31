# Invigilator Frontend Guide

Use this with the Postman collection in this folder to build the invigilator UI.

## Import into Postman

1. Import `Invigilator.Dashboard.postman_collection.json`
2. Import `Invigilator.local.postman_environment.json`
3. Select environment **Invigilator Local**
4. Start the backend (`./mvnw spring-boot:run`)
5. Run **1. Auth → Login as Invigilator** first (saves `accessToken`)
6. Run the remaining folders in order

## Demo credentials

| Field | Value |
|-------|-------|
| Email | `invigilator@unza.zm` |
| Password | `Invig@2026` |
| Default exam | CS101 → `examSessionId = 5` |
| Default venue | Main LT 1 → `venueId = 16` (must match allocation for PRESENT) |
| Students at that venue | `2022004264`, `2022004265`, `2022004266` |

Use `venueId = 16` when checking in those students. Checking in at a different venue marks `WRONG_VENUE`.

Each invigilator has at most one assignment per overlapping time slot (no double-booking).

Secondary invigilator (Business Hall / EE221): `invigilator2@unza.zm` / `Invig2@2026`

## Response envelope

Every API returns:

```json
{
  "success": true,
  "message": "...",
  "data": {}
}
```

On errors: `success: false`, `data: null`, HTTP 400 / 409 / 401 / 403.

Auth header after login:

```http
Authorization: Bearer <accessToken>
```

## Screens the frontend should build

### 1. Login
- `POST /api/auth/login`
- Body: `{ "email", "password" }`
- Store `data.accessToken`

### 2. Dashboard home
- `GET /api/dashboard/invigilator`
- Show cards: assignedExaminations, assignedVenues, checkedInStudents, absentStudents, scriptsCollected, incidents
- Primary CTA → Check-in
- Secondary CTA → Attendance register

### 3. My assignments / start session
- `GET /api/invigilator/assignments` → populate exam + venue selectors
- Each assignment includes `lecturers[]` with the course lecturer(s): `staffId`, `staffNo`, `fullName`, `email`, `department`
- Show lecturer name/email on the assignment card so invigilators know who the exam belongs to
- `POST /api/invigilator/assignments/{examSessionId}/{venueId}/start`
- Do **not** invent exam IDs; only use returned assignments
- `POST /api/invigilator/assignments/{examSessionId}/{venueId}/end` → sets `COMPLETED` and marks remaining allocated students `ABSENT`
- Timer UX: countdown to `examDate` + `endTime`; when zero (or after End), expect `examStatus: "COMPLETED"`
- Backend also auto-completes `IN_PROGRESS` exams past `end_time` about every 30s

### 4. Check-in flow (MVP = computer number)
1. Select exam + venue from assignments
2. Start session (must be `IN_PROGRESS`)
3. Enter computer number
4. `GET /api/attendance/lookup?computerNumber=&examSessionId=`
5. Show photo, name, computer number, programme, allocated venue, seat
6. Invigilator verifies visually
7. `POST /api/attendance/check-in` with `{ computerNumber, examSessionId, venueId, verificationMethod: "COMPUTER" }`
8. Handle 409 duplicate: “already checked in”; check-in fails if session is not `IN_PROGRESS`

### 5. Attendance register
- `GET /api/attendance/exam/{examSessionId}`
- Optional summary: `GET /api/attendance/exam/{examSessionId}/summary`
- Optional scripts: `POST /api/attendance/exam/{examSessionId}/scripts-collected` body `{ "count": N }`

### 6. Incidents
- Report: `POST /api/incidents`
- List: `GET /api/incidents`

Allowed `incidentType`:

`CHEATING` | `PHONE_FOUND` | `WRONG_VENUE` | `MEDICAL_EMERGENCY` | `DISTURBANCE` | `LATE_ARRIVAL` | `OTHER`

Severity: `MINOR` | `MAJOR` | `CRITICAL`

`computerNumber` and `evidencePath` are optional.

### 7. Generate report (optional)
- `POST /api/reports/exam-session/{examSessionId}`
- Returns report metadata; PDF download is not exposed to invigilator in this phase

## Recommended UI navigation

- Dashboard
- Check-in
- Attendance register
- Incidents
- Generate report

Do **not** build student management, venue allocation, exam creation, or staff admin screens for this role.

## Important backend rules

- Invigilator only sees **seeded assignments** (already assigned outside this app)
- Check-in and incident reporting require assignment to that exam+venue
- Student must be **allocated** before lookup/check-in
- Duplicate attendance for the same student+exam is never allowed (HTTP 409)
- Dashboard incident count is **real incidents**, not “non-PRESENT attendance”

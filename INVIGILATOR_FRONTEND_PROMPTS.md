# Invigilator Dashboard — Frontend Prompts

These prompts are grounded in the currently implemented backend. They are intentionally scoped to the APIs an `INVIGILATOR` can call, so the UI does not promise actions the server cannot complete.

## What the invigilator can do

| User task | Endpoint | UI implication |
| --- | --- | --- |
| See dashboard summary | `GET /api/dashboard/invigilator` | Show the `assignedExaminations` metric only. |
| Check in a student | `POST /api/attendance/check-in` | Provide the primary operational workspace. |
| Review an exam's attendance | `GET /api/attendance/exam/{examSessionId}` | Show a filterable attendance register for a chosen exam ID. |
| Review flagged records | `GET /api/incidents` | Show all non-`PRESENT` attendance records. |
| Generate an attendance PDF | `POST /api/reports/exam-session/{examSessionId}` | Make this a deliberate action with success/error feedback. |

All protected requests must send `Authorization: Bearer <accessToken>`. API responses are wrapped as `{ success, message, data }`.

## Master prompt: build the complete dashboard

> Build a responsive, accessible invigilator dashboard for a university Digital Examination Attendance System. The user is an authenticated `INVIGILATOR`. Create a calm, focused operations interface rather than an admin analytics dashboard: students must be checked in quickly with minimal friction during an exam.
>
> Use a left navigation rail on desktop and a compact mobile menu. The navigation has Dashboard, Check-in, Attendance register, Incidents, and Generate report. The header shows “Invigilator workspace”, the current date/time, a notification placeholder, and a profile/logout menu. Do not include student management, venue allocation, exam creation, staff administration, or report-library screens; those are not available to this role in the backend.
>
> Dashboard route: call `GET /api/dashboard/invigilator` and display `data.assignedExaminations` in one prominent metric card. Pair it with a concise “Start check-in” primary action and “View attendance register” secondary action. Since the API returns no actual exam-session list, display a clearly labelled Exam session ID input wherever an exam must be selected; do not fabricate an “upcoming exams” feed.
>
> Check-in route: make this the central workflow. At the top, show an Exam session ID field and a Venue ID field that stay visible. Under it, place a large computer-number field optimized for scanner/keyboard entry, with a verification-method selector. Valid values are `COMPUTER`, `QR_CODE`, `FACE_RECOGNITION`, `QR_AND_FACE`, and `QR_AND_FACIAL`. On submit, call `POST /api/attendance/check-in` with `{ computerNumber, examSessionId, venueId, verificationMethod }`. Enforce the computer-number format of exactly 10 digits, where the first four digits form the year. On success, show a high-visibility green confirmation card with the student name and computer number when provided, check-in time, status, venue, and verification method; then clear and refocus the computer-number input for the next student. On error, keep the entered values, give a specific accessible error message, and distinguish duplicate check-in, unknown student, invalid exam/venue, unsupported verification method, and validation failures when the API message permits it.
>
> Attendance register route: require an Exam session ID, then call `GET /api/attendance/exam/{examSessionId}`. Render a dense but readable table with computer number, student name, check-in time, venue, verification method, attendance status, scripts submitted, and alert message. Include client-side search, status filters, verification-method filters, sortable check-in time, loading skeletons, an empty state, and an error/retry state. Use status chips: green for `PRESENT`, amber for `LATE`, red for `ABSENT` and `WRONG_VENUE`. Do not include edit, delete, manual-status override, or script-submission controls because no invigilator API supports them.
>
> Incidents route: call `GET /api/incidents`; this endpoint returns attendance records whose status is not `PRESENT`, not separately created incident reports. Explain this in the page subtitle (“Flagged attendance records”). Reuse the table design, show the related exam-session ID, and provide client-side filters for status, venue, and search. Do not offer “Create incident” or incident severity fields.
>
> Generate-report route: request an Exam session ID and use a guarded “Generate attendance PDF” button. On confirmation call `POST /api/reports/exam-session/{examSessionId}`. Show progress, then a success card using returned report metadata such as title, generated time, report type, and summary. Do not add a download button unless the returned `filePath` is explicitly exposed through a downloadable backend route; none exists now.
>
> Build an API client that unwraps the response envelope and adds the JWT bearer token. If a request returns 401 or 403, redirect to login or show an authorization message. Use clean university branding, generous contrast, keyboard-first forms, visible focus indicators, semantic labels, non-colour-only status cues, and responsive layouts. Use realistic placeholder data only in loading/demo states and make it visually clear that it is not live data.

## Focused implementation prompts

### 1. Check-in screen

> Create the “Check-in student” screen for an invigilator. Prioritize speed, error prevention, and scanner-friendly data entry. Place persistent Exam session ID and Venue ID controls in a top context bar. Below, use a single large autofocus field for a 10-digit computer number, a verification-method dropdown with the API enum values, and a full-width “Check in student” submit button. Add inline validation before submission and preserve context after any error. Connect the form to `POST /api/attendance/check-in`, unwrap `{ success, message, data }`, and present the returned attendance record in a success panel. On a successful check-in, reset only the computer number and refocus it. Handle duplicate submission gracefully and prevent repeated clicks while the request is pending. Do not add camera, QR decoding, facial recognition, student lookup, or seat validation features: the current backend accepts only the typed request values.

### 2. Attendance register

> Create an invigilator attendance register that loads after the user enters an Exam session ID and clicks “Load register”. Fetch `GET /api/attendance/exam/{examSessionId}` with bearer authentication. Display a responsive table/card list of the returned attendance records. Include computer number, student name, check-in date/time, venue, verification method, attendance status, scripts submitted, and alert message. Add local search and filters; do not claim that the filters query the server. Show a clear empty state for an exam with no records. Do not add record mutation actions, exports, or pagination API calls because the backend provides none.

### 3. Flagged attendance records

> Create a page titled “Flagged attendance records”, not “Incident reports”. Fetch `GET /api/incidents` for an invigilator and explain that it lists attendance records with a status other than `PRESENT`. Show status, student, exam session ID, venue, checked-in time, verification method, and alert message. Use red/amber status treatment and filters. Do not include a create-incident form, severity field, resolution workflow, or incident editing because they are not exposed by the API.

### 4. Report generation

> Create a compact “Generate attendance report” page for an invigilator. Request an Exam session ID, explain that report generation may take a moment, and ask for confirmation before sending `POST /api/reports/exam-session/{examSessionId}`. Display pending, error, and success states. After success, show only metadata returned from the generated report. Do not promise a direct PDF download or report history: the invigilator can generate a report, but no download endpoint or report-list endpoint is currently authorized for that role.

## API integration details

### Login and role guard

`POST /api/auth/login`

```json
{ "email": "...", "password": "..." }
```

Store `data.accessToken` for authenticated requests. The token contains role authorities; show this workspace only when it includes `INVIGILATOR`. There is no refresh-token endpoint in the current backend, despite the login response containing a refresh token, so return the user to login when their access token expires.

### Check-in request

```json
{
  "computerNumber": "2023123456",
  "examSessionId": 4,
  "venueId": 1,
  "verificationMethod": "QR_CODE"
}
```

The request uses numeric IDs; the backend does not currently provide an invigilator endpoint for looking up available exam sessions or venues. Until those endpoints exist, use labelled ID entry, or hydrate selectors only from another agreed data source.

## Backend constraints to surface to the product team

- `GET /api/dashboard/invigilator` returns a system-wide exam count as `assignedExaminations`; it is not filtered to the logged-in invigilator.
- Check-ins are attributed by the controller to the hard-coded staff email `invigilator@unza.zm`, not the logged-in invigilator.
- Every successful check-in is currently assigned `PRESENT`; the service does not calculate late or wrong-venue status.
- The “incidents” endpoint is a derived list of non-present attendance records. It does not create or return the separate incident entity described in the data model.
- Report generation currently uses venue ID `1` and the hard-coded staff email `admin@unza.zm` internally.
- There are no exposed APIs to list exam sessions, venues, allocations, students, or an invigilator's assignment; no endpoint supports attendance updates, script-submission updates, or report downloads.

Avoid masking these gaps with fake live UI. The smallest backend additions that would unlock a richer operational dashboard are: an authenticated “my assignments” endpoint, session/venue lookup endpoints, authenticated staff attribution, attendance-status logic, incident CRUD, and a report download endpoint.

# Student Dashboard

The Student dashboard shows only the signed-in student's eligible, published exams
and examination-pass information. Student identity comes from the bearer token. Do not
request a computer number to choose whose examinations or pass to show.

## Endpoints

| Endpoint | UI use |
| --- | --- |
| `GET /api/student/examinations` | List this student's eligible published examinations. |
| `POST /api/student/examination-pass?academicYear={year}&semester={semester}` | Generate or refresh pass data for optional cycle filters. |
| `GET /api/student/examination-pass?academicYear={year}&semester={semester}` | Retrieve pass details for optional cycle filters. |
| `GET /api/student/examination-pass/pdf?academicYear={year}&semester={semester}` | Download the PDF pass; response is binary, not the JSON envelope. |
| `GET /api/examination-notifications` | Read available notices for this student account. |
| `POST /api/examination-notifications/{id}/read` | Mark only this student's available notice as read. |

The academic-year and semester filters are optional. All JSON responses use the
standard envelope. Typed student responses use camelCase.

## Examination List

Each examination summary may include `examSessionId`, `courseCode`, `examDate`,
`startTime`, `endTime`, `academicYear`, `semester`, `examType`, `examStatus`,
`allocated`, `venueName`, `building`, `passGenerated`, `passId`, and
`attendanceStatus`. Use the server response as the source of truth after an amendment;
refresh the list and pass details rather than editing local placement data.

Only the authenticated student's eligible published examinations are returned.
Eligibility depends on the existing account, course registration, programme enrolment,
and pass rules. An allocation by itself does not establish exam eligibility. Drafts
must not appear in the examination list, pass details, or PDF.

## UI Boundaries and States

- Do not expose Administrator scheduling controls or other students' data.
- Show allocated venue information only when returned for the student's eligible
  examination. Do not infer or guess a venue from an allocation that is not present.
- Keep pass generation explicit and show the returned generation/expiry information.
  Do not promise a pass when the backend reports account or enrolment requirements
  are not met.
- After an amendment notification, refresh the examination list and pass details to
  obtain the latest arrangement.
- Clear examination and pass data on logout/account change. Cache by authenticated
  student and cycle filters.
- Show distinct loading, no-eligible-exams, pass-not-yet-generated, and server-error
  states. Preserve backend error messages where suitable for the user.

## Acceptance Checks

- A student receives only their own eligible published examinations; drafts never
  appear.
- Pass generation and retrieval preserve existing account, registration and
  programme-enrolment constraints.
- Another student's notification cannot be listed or marked read with this account.
- After an applied timetable amendment, refreshing shows the new exam date/time and
  venue details.

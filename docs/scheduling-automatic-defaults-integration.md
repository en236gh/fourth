# Frontend handoff: automatic scheduling academic year

## Required behavior

The administrator does not choose an academic year. Remove the editable year input,
year dropdown, and client-side year validation. Display the year as read-only text.
Keep semester, exam type, dates, weekdays, and daily slots as user inputs.

New schedules always use `max(student_registration.academic_year)` across all
registrations, independent of the selected semester. The backend ignores any
legacy `academicYear` value sent by a client, including a conflicting value.
It does not derive the academic year from today's date or the examination dates.

When the registration table contains only `2025/2026`, new schedules automatically
use `2025/2026`. Do not hardcode that year in the frontend. If `2026/2027`
registrations remain, the backend selects that later year. Removing those rows is
a separate data-cleanup step; the code change does not delete registrations.

## Display the year before creation

Call `GET /api/admin/examination-periods/defaults` using administrator authentication.
Read `data.academicYear` from the standard API envelope:

```json
{
  "data": {
    "academicYear": "2025/2026"
  }
}
```

This example shows only the relevant envelope field. Display:

> Academic year: 2025/2026 — Automatically taken from student registrations.

Refresh this preview when opening new schedule setup, especially after registration
cleanup. Creation reads registrations again; the preview is not authoritative.
If defaults fails, show the server's message. Do not substitute a calendar year or
reintroduce manual entry. Empty registrations or an invalid latest year cause
HTTP 409 on defaults and creation.

## Create and update payloads

Omit `academicYear` entirely:

```http
POST /api/admin/examination-periods
Content-Type: application/json

{
  "name": "Semester 1 final examinations",
  "semester": 1,
  "examType": "FINAL",
  "startDate": "2026-10-05",
  "endDate": "2026-10-16",
  "daysOfWeek": [1, 2, 3, 4, 5],
  "slots": [
    {"startTime": "09:00", "endTime": "12:00"},
    {"startTime": "14:00", "endTime": "17:00"}
  ]
}
```

Use the same setup fields for
`PUT /api/admin/examination-periods/{id}?revision={revision}`.
Existing setup-edit restrictions still apply.

After creation, display the saved year from
`data.generation.period.academic_year`. For detail and setup-update responses,
use `data.academic_year`. Note the different names: defaults uses `academicYear`;
saved periods use `academic_year`.

Only one schedule per academic year, semester, and exam type can be created.
If creation reports an existing cycle, open the existing schedule from list/detail
instead of retrying creation with a different year.

## Existing schedules

Existing schedules retain their saved academic year. Updating setup or regenerating
a schedule does not change that year, even after registrations roll over or are
deleted. Display the saved year when an existing schedule is selected.

In particular, a draft saved as `2026/2025` is not repaired by this change or by
registration cleanup. It still needs separate correction or removal through an
authorized data-maintenance process; the scheduling API has no delete/reset action.
Do not present its regeneration as creating a new current-year schedule.

## Registration counts and failures

Generation discovers active courses with registrations matching both the saved
academic year and selected semester. Required seats come from those registrations,
without an attendance or exam-pass requirement. Do not ask for a student count.

Use `eligible_students` for course demand, `registered_students` for generated
exam demand, and `allocated_students` for completed allocations. Venue
`examination_capacity` remains a separate physical seating limit.

Zero generated exams or allocations does not establish that registrations are
missing. On generation failure, show `data.generation.result.problems` visibly,
including the selected schedule's year and semester. Preserve the returned period
and revision. `INVALID_INPUT` can also indicate invalid time slots or existing
same-cycle examinations. Do not replace the explanation with a generic
“No registrations” message.

## Verification

- With only `2025/2026` registrations, defaults and a newly created schedule show
  `2025/2026`, regardless of examination dates.
- No editable academic-year control appears; create and update omit the field.
- A legacy client sending a past, future, or reversed year cannot override the
  backend's registration-derived year on creation.
- The saved response year is displayed after creation, including when generation
  returns a failure with a retained period.
- Empty registrations show the server's explanatory error rather than a fallback.
- Opening an existing schedule displays its saved year rather than the new-period
  preview.

Backend regression tests and frontend scheduling tests cover the automatic-year
contract. The administrator dashboard already removes the field and omits it from
setup payloads; these instructions define the contract for other frontend clients.

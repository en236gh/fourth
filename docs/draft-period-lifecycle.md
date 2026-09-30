# Administrator-triggered automatic scheduling

The administrator enters the academic cycle and exam window, then clicks **Generate schedule**. One request discovers exams, schedules times and venues, allocates registered students, and assigns available invigilators. The administrator reviews the result and clicks **Publish** after validation passes.

This repository contains the backend only. The separate administrator dashboard must wire the button to the API below and remove its course-selection, draft-delete, and reset-draft controls. Automation runs only on an administrator request; it is not a background job.

## Generate schedule button

```http
POST /api/admin/examination-periods
Authorization: Bearer <administrator-token>
Content-Type: application/json

{
  "name": "Semester 1 final examinations",
  "academicYear": "2026/2027",
  "semester": 1,
  "examType": "FINAL",
  "startDate": "2027-06-07",
  "endDate": "2027-06-18",
  "daysOfWeek": [1, 2, 3, 4, 5],
  "slots": [
    {"startTime": "09:00", "endTime": "12:00"},
    {"startTime": "14:00", "endTime": "17:00"}
  ]
}
```

`academicYear` is honored when supplied. If omitted, the latest academic year in `student_registration` is used; `GET /api/admin/examination-periods/defaults` supplies that default. Always display the resolved cycle. One schedule per academic year, semester and exam type can be created through the service. If it already exists, open it using the list/detail APIs and review or regenerate it. Existing same-cycle records from the previous workflow are preserved.

The response uses the existing API envelope, with this new `data` structure:

- `generation.result`: `outcome`, `placements`, `unresolvedCourses`, `searchSteps`, and `problems`.
- `generation.period`: the schedule with `period_id`, current `revision`, discovered courses, exams, bookings, student allocations, and assignments.
- `staffing`: per-exam assignment results, including `understaffedVenueIds`.
- `validation`: `valid` and publication `problems`.

`success` means timetable generation completed. It does not mean the schedule can be published: inspect `validation.valid` and show staffing shortages. Use the returned schedule ID for subsequent actions, even when generation fails. Disable the button while its request is pending. After a transport error, reload the schedule list before retrying creation.

## Source data and review

Only active catalog courses with at least one registration matching the schedule's exact academic year and semester become exams. Registration rows have no active/inactive or exam-type eligibility field in this schema. The same cycle registration rule currently applies to all exam types. Empty or mismatched registration data produces an actionable failure; the system never invents students or seats from programme membership.

There is no catalog exam-duration field. Generation uses `app.scheduling.exam-duration-minutes` (default `120`, valid range 1–1440) for all exams. Configure this before generation. Only venues with verified `examination_capacity` are used. The scheduler respects shared students, existing exams, venue reservations and venue unavailability. Invigilators must be active with the `INVIGILATOR` role and free of overlapping assignments; workload is used to distribute duties. Separate qualification and staff-availability calendars are not modeled.

Timetable generation, allocations and automatic staffing run in one transaction. A scheduling failure saves no partial timetable; an initial attempt retains its schedule setup for correction. Staffing shortages retain the generated timetable for review. An unexpected staffing error rolls back the transaction.

The database's existing `DRAFT` status means **unpublished / awaiting review**. It is retained for compatibility with publication guards and student visibility, not as a separate administrator course-selection step. `examination_period_course` stores the automatically generated course snapshot.

## Correct and regenerate

Fix registrations, catalog data, venue capacities or unavailable intervals, then call:

```http
POST /api/admin/examination-periods/{id}/generate
Content-Type: application/json

{"revision": 1, "searchLimit": 100000}
```

Use the latest revision from the response or detail endpoint. A successful regeneration replaces unpublished exams, allocations and unpublished staffing, then assigns invigilators again. Failed searches preserve the existing timetable, course snapshot, staffing and revision. Published or operational exams cannot be regenerated. Empty setup records can be edited with the existing `PUT /{id}?revision=...` endpoint; generated schedules can be reviewed through placement edits and staffing controls.

Removed routes: `PUT /{id}/courses`, `DELETE /{id}`, and `POST /{id}/reset-draft`. Reading catalog counts remains available for data review, but does not select exams. There is no separate required auto-assignment step; the existing staffing action remains available to fill shortages after staff data is corrected.

## Publish button

```http
POST /api/admin/examination-periods/{id}/publish
Content-Type: application/json

{"revision": 1}
```

Use `GET /{id}/validation` to review current problems. Publishing checks current course discovery, registration coverage, capacity, timetable conflicts and staffing again. Newly registered courses require regeneration; newly registered students require matching allocations. Successful publication exposes exams and allocations to students and publishes invigilator assignments together. Generation alone never exposes an unpublished timetable to students.

No schema migration is required for this API change. Existing scheduling migrations and database safeguards remain in effect. Deploy the dashboard update with the backend because the creation/generation response shape and removed routes are breaking changes.

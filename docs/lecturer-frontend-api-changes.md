# Lecturer frontend: course ownership and examination viewing

For a consolidated explanation of the latest seat-number and PDF changes, see
[Seat-number removal and attendance PDF report changes](seat-number-and-pdf-report-changes.md).

## What changed

Lecturer access is now based on the signed-in staff member's entries in
`course_lecturer`. A lecturer can view registrations only for
examinations belonging to their assigned courses. Read endpoint URLs remain
the same, and `GET /api/exams/my-courses` provides course codes. The lecturer
student-to-venue assignment POST has been removed, along with its allocation logic.

Previously, the exam list and lecturer dashboard included data across all courses.
They now include only the lecturer's assigned courses. Requests for another course's
registrations, venues, or allocation statistics return HTTP `403`
for lecturer accounts. Administrator read access is unchanged.

Every request below uses the existing login token:

```http
Authorization: Bearer <accessToken>
```

Do not send a lecturer ID, email, or course code to select the acting lecturer.
The backend resolves ownership from the token. A lecturer can have multiple assigned
courses; do not hard-code a single course into the frontend.

## Endpoint changes

After V30, `GET /api/exams/my-course-hierarchy` returns the signed-in lecturer's
active curriculum rows in `data`: `school_id`, `school_name`, `programme_id`,
`programme_code`, `programme_name`, `major_id`, `major_code`, `major_name`,
`programme_course_id`, `year_of_study`, `semester`, `course_code`, `course_name`.
Major fields may be null for programmes without majors. Build the school ->
programme -> major -> year/semester -> course selectors from these rows. Shared
courses have several curriculum rows but only one lecturer/course ownership link.
Use `GET /api/exams` to select the actual exam ID for a course, then fetch its
registered students and venues for viewing.

For SQL setup and readiness checks after V30, see
[Lecturer course allocation](lecturer-course-allocation.md#after-the-programme-major-migration).

| Method and URL | Lecturer behavior / frontend action |
| --- | --- |
| `GET /api/exams/my-courses` | New. Returns an array of assigned course-code strings. Use for course labels or filters. |
| `GET /api/exams` | Returns only exams for assigned courses. Populate the examination selector from this response. |
| `GET /api/exams/{examSessionId}/registered-students` | Returns registrations only when the lecturer owns the course. |
| `GET /api/exams/{examSessionId}/venues` | Returns linked venues only when the lecturer owns the course. |
| `POST /api/allocation/exam-session/{examSessionId}` | Removed. Do not call this endpoint; lecturers no longer assign students to venues. |
| `GET /api/allocation/exam-session/{examSessionId}` | Returns venue assignments and allocation totals for an assigned course. |
| `GET /api/dashboard/lecturer` | Totals only assigned-course exams; adds `courseCodes`. |
| `GET /api/dashboard/lecturer?examSessionId={examSessionId}` | Returns allocation statistics for one assigned exam. |

Use `examSessionId` from the exam list in all examination URLs, not `courseCode`.
The same course can have multiple examination sessions.

## Response examples

All JSON endpoints use `{ "success": true, "message": "...", "data": ... }`.
The examples below illustrate the response shape; IDs and counts are not fixed seed values.

### Assigned courses

`GET /api/exams/my-courses`

```json
{
  "success": true,
  "message": "Assigned courses retrieved",
  "data": ["CSC1202"]
}
```

The response contains course codes, not course-name objects.

### Examination selector

`GET /api/exams`

```json
{
  "success": true,
  "message": "Exam sessions retrieved",
  "data": [
    {
      "examSessionId": 22,
      "courseCode": "CSC1202",
      "examDate": "2026-10-05",
      "startTime": "13:00:00",
      "endTime": "15:00:00",
      "academicYear": "2026/2027",
      "semester": 1,
      "examType": "FINAL",
      "status": "SCHEDULED"
    }
  ]
}
```

Registration rows retain `computerNumber`, `fullName`, `program`, `yearOfStudy`,
`photoPath`, and `status`. Venue rows retain `venueId`, `venueName`, `building`,
and `capacity`. Both endpoints return arrays inside `data`.

### Dashboard without an exam selection

`GET /api/dashboard/lecturer`

```json
{
  "success": true,
  "message": "Lecturer dashboard",
  "data": {
    "courseCodes": ["CSC1202"],
    "totalExaminations": 1,
    "registeredStudents": 30,
    "allocatedStudents": 25,
    "message": "Pass examSessionId to view venue allocation statistics for a specific examination"
  }
}
```

`registeredStudents` counts registrations per exam session, and `allocatedStudents`
counts venue allocations per exam session. These are not distinct student headcounts
across all courses or exam sessions.

### Student venue assignment removed

Remove the assignment/allocation button, confirmation dialog, and POST call from
the lecturer frontend. Existing assignments are available for read-only viewing.
The removed POST returns HTTP 405 for an authenticated request reaching this route.

### Allocation details

`GET /api/allocation/exam-session/22`

```json
{
  "success": true,
  "message": "Allocation statistics retrieved",
  "data": {
    "examSessionId": 22,
    "registeredStudents": 1,
    "allocatedStudents": 1,
    "totalVenueCapacity": 100,
    "venueFills": [
      { "venueId": 1, "venueName": "Demo Hall", "capacity": 100, "allocated": 1 }
    ],
    "allocations": [
      {
        "computerNumber": "2022004264",
        "studentName": "Demo Student",
        "venueId": 1,
        "venueName": "Demo Hall"
      }
    ]
  }
}
```

`GET /api/dashboard/lecturer?examSessionId=22` instead returns
`data: { "examSessionId": 22, "allocation": { ... } }`, where `allocation` has
the statistics shape above. It does not include the overall dashboard fields.

## Frontend flow and state handling

1. After login, load assigned courses, exams, and the lecturer dashboard.
2. Populate the exam selector only from `GET /api/exams`. Display course, date,
   and time so sessions of the same course can be distinguished.
3. When an exam is selected, load its registered students, venues, and current
   allocations. Clear the previous exam's data while loading; ignore stale responses
   if the selection changes before a request completes.
4. Do not show student venue assignment controls or call the removed allocation POST.
5. Clear lecturer-specific cached courses, exams, students, and allocations on
   logout or account change. Include account identity and exam ID in cache keys.

If `my-courses` is empty, show "No courses assigned. Contact the administrator."
and show an empty examination view. If courses exist but the exam list is empty, show
"No examinations available for your assigned courses." A course assignment alone
does not create an exam, register students, or link venues.

## Error handling

An ownership rejection has this shape:

```json
{
  "success": false,
  "message": "You are not assigned to this examination's course",
  "data": null
}
```

| HTTP status | Meaning / frontend action |
| --- | --- |
| `403` | Course access denied. Clear the inaccessible exam details, show the message, and refresh the exam selector. |
| `400` | Examples: `Exam session not found`, `No registered students found for this examination`, or `No venues linked to this examination`. Show the backend message. |

Continue using the application's existing expired-token/session handling.

## Demo setup and frontend acceptance checks

The account-to-course mappings and SQL execution order are in
[Lecturer course ownership and examination viewing](lecturer-course-allocation.md).
The database scripts must be applied before testing; Flyway is disabled, so a backend
restart alone does not seed the assignments.

- Sign in as `lecturer2@gmail.com`: assigned courses include `CSC1202`; the exam
  selector contains only that account's assigned-course exams.
- Select an assigned exam and verify registrations, venues, and existing venue assignments load.
- Confirm there is no student venue assignment button and POST requests are rejected.
- Attempt another lecturer's exam ID: registration/detail requests
  return `403`, and the UI does not retain the previous student's information.
- Switch lecturer accounts and verify cached exam/student data is cleared.
- Test an account without course assignments and an assigned course without exams.

Existing assignments are preserved by the seed scripts, so an account with additional
course links can legitimately see more than its single demo course.

## Seat numbers removed

Existing student venue assignments have no numbered seats. Lecturers can view
these records but can no longer create or replace them.
Remove seat-number fields and columns from student screens, attendance lookups,
allocation details, and examination passes. These responses no longer contain
`seatNumber`, and generated PDF passes no longer display a Seat column.

For existing databases, apply `V31__remove_seat_numbers.sql` after earlier migrations
and legacy demo seeds. Flyway is disabled, so run this migration manually.
Historical migrations and the database snapshot retain the old column for replay;
do not rerun historical seat-number seeds after V31. The Phase 12 venue seed is
compatible with the updated schema.

## Attendance and incident PDF download

`GET /api/reports/exam-session/{examSessionId}/pdf` downloads one PDF containing
separate present/attending and absent student lists, followed by incidents.
The UNZA logo appears above the report title. The endpoint and download handling
are unchanged; no additional frontend requests are needed.

The attending list includes PRESENT, LATE and WRONG_VENUE check-ins. The absent
list includes recorded ABSENT students and their assigned venues. Absences are
finalized by the existing end-examination process, so download after completing
the exam for the final absence list. No seat numbers appear in either list.

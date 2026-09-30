# Lecturer Dashboard

The Lecturer dashboard is a read-only view of examinations for courses assigned to
the signed-in lecturer, plus a form for submitting a post-publication change request.
The backend resolves the lecturer from the bearer token; do not send a lecturer ID to
select data.

## Endpoints

| Endpoint | UI use |
| --- | --- |
| `GET /api/dashboard/lecturer` | Assigned-course totals and per-exam statistics. |
| `GET /api/dashboard/lecturer?examSessionId={id}` | Statistics for one owned exam under `data.allocation`. |
| `GET /api/exams/my-courses` | Assigned course-code list. |
| `GET /api/exams/my-course-hierarchy` | Assigned curriculum hierarchy for filters, if needed. |
| `GET /api/exams` | Published examinations for assigned courses; use `examSessionId` for details. |
| `GET /api/exams/{examSessionId}/registered-students` | Registered students for an owned exam. |
| `GET /api/exams/{examSessionId}/venues` | Venues for an owned exam. |
| `GET /api/allocation/exam-session/{examSessionId}` | Read-only venue fills, allocations and counts. |
| `GET /api/examination-change-requests?periodId={id}` | This lecturer's requests for published courses in the period. |
| `POST /api/examination-change-requests` | Submit a timetable review request. |
| `GET /api/examination-notifications` | Read available account-scoped amendment notices. |
| `POST /api/examination-notifications/{id}/read` | Mark the lecturer's own notice as read. |

All JSON endpoints use the shared response envelope. Typed exam, allocation and
request DTOs use camelCase. Change-request rows returned from JDBC use snake_case.

## Dashboard Data

The overall dashboard includes `courseCodes`, `totalExaminations`,
`registeredStudents`, `allocatedStudents`, `unallocatedStudents`, `attendedStudents`,
and `examinations`. Counts represent student/exam participations, not unique people
across all courses or sessions. Per-exam allocation stats include
`invalidAllocationRecords`; surface these for Administrator follow-up instead of
folding stale rows into valid allocation counts.

For a selected exam, `/api/dashboard/lecturer?examSessionId={id}` returns
`{ "examSessionId": id, "allocation": { ... } }`, not the overall dashboard shape.
Use the exam-session ID, not a course code, because one course can have multiple
sessions.

## Change Request Flow

Submit:

```json
{
  "periodId": 4,
  "courseCode": "DEMO101",
  "examSessionId": 12,
  "proposedChange": "Please review the examination date",
  "reason": "The published date conflicts with an approved academic event"
}
```

`examSessionId` may be null. If provided, the exam must match the period and course.
Only lecturers assigned to the course may submit or view their own requests for that
published course. Display the returned status, decision and timestamps. A request is
not a timetable change; only an Administrator can decide it, and even approval does
not edit the timetable.

## UI Boundaries

- Do not show student venue assignment or move controls. The old lecturer allocation
  POST operation is removed; existing allocations are read-only.
- Do not show unpublished drafts or exams belonging to another lecturer.
- On `403`, clear selected exam, student, venue and allocation state, then refresh the
  owned exam selector. Do not leave prior exam data visible after a denied request.
- Clear lecturer data on logout or account change. Cache by account and exam ID.
- Use an explicit empty state for no assigned courses and a separate one for courses
  with no published exams.

## Acceptance Checks

- Two lecturers with different assignments see different exam selectors and counts.
- A lecturer cannot load another lecturer's exam details, registrations, venues or
  allocation stats.
- There are no student-to-venue assignment controls or POST calls.
- Submitting a request leaves timetable placement unchanged; returned decision state
  appears after refresh.

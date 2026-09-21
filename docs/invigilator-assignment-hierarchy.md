# Invigilator assignment hierarchy

Administrators select School -> Programme -> Year of study -> Course -> Exam -> Venue.
An exam contains the academic year, semester, date and exam type. Shared exams
remain a single duty even when linked to several programmes.

All lookup routes use the prefix `/api/admin/invigilator-assignments/academics`:

| GET route | Required query parameters |
| --- | --- |
| `/schools` | none |
| `/programmes` | `schoolId` |
| `/years` | `schoolId`, `programmeId` |
| `/courses` | `schoolId`, `programmeId`, `yearOfStudy` |
| `/exams` | `schoolId`, `programmeId`, `yearOfStudy`, `courseCode` |

Responses use the existing ApiResponse envelope; lookup row fields use database
names such as `school_id`, `programme_id`, `year_of_study`, `course_code`,
`semester`, `academic_year` and `exam_session_id`. Courses include their semester.
Exam results exclude completed exams and require an explicit curriculum mapping
with the same course code and semester as the exam. All catalog parents must be active.
Use `GET /api/exams/{examSessionId}/venues` for the selected exam's venues.

## Manual assignment (breaking request change)

`POST /api/admin/invigilator-assignments`

```json
{
  "selection": {
    "schoolId": 1,
    "programmeId": 2,
    "yearOfStudy": 3,
    "courseCode": "CSC3101"
  },
  "examSessionId": 10,
  "venueId": 1,
  "staffId": 2,
  "notes": "Main venue"
}
```

The selection is required. A mismatch returns HTTP 400 before creating an assignment.
Existing venue-link, active-invigilator and timetable-conflict checks still apply.

## Automatic assignment (breaking request change)

`POST /api/admin/invigilator-assignments/exam-sessions/10/auto-assign`
now requires the selection directly as its JSON body:

```json
{"schoolId": 1, "programmeId": 2, "yearOfStudy": 3, "courseCode": "CSC3101"}
```

## Database rollout

Flyway V27 creates missing academic catalog and exam-mapping tables without
replacing existing Supabase data. Empty catalogs intentionally offer no selections
and unmapped exams cannot receive new assignments. Populate approved schools,
programmes, courses, curriculum entries and explicit exam-to-curriculum mappings
before using this flow. No relationships are inferred from students' current years
or from course-code naming conventions. The phase 3 seed script is demo data only.
Existing assignments and the publish/cancel endpoints are unchanged.

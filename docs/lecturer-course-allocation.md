# Lecturer course ownership and venue allocation

For frontend integration, response examples, and UI changes, see
[Lecturer frontend API changes](lecturer-frontend-api-changes.md).

Run `V28__seed_additional_lecturers.sql`, then `V29__seed_lecturer_course_assignments.sql`
in the SQL editor against the existing database. The original lecturer account from
`V27__seed_requested_staff_accounts.sql` and the academic catalog tables must exist.
Flyway is currently disabled, so restarting the application does not apply these scripts.

V29 only inserts lecturer-to-course links using existing catalog courses from
`supabase/phase_3_seed_academic_catalog.sql`. It does not create or update courses.
It checks that all five accounts and courses exist, with active curriculum mappings.

## Existing academic and staff relationships

The catalog hierarchy is School -> Programme -> Major -> Year of study / Semester -> Course.
`programme.school_id` links programmes to schools, and `programme_course` places a
course within a programme, year of study, and semester. V29 requires an existing
active curriculum path for each assigned course; it does not create catalog records.
After V30, `programme_course.major_id` identifies the major within its parent
programme. Education entries can still have no major.

The current lecturer link is `staff -> course_lecturer -> course`. Joining that course
to `programme_course` exposes its programme and school context. This is currently
course-wide ownership: when a course appears in multiple programmes, the lecturer
has access to that course's exams across those programmes, not just one programme.

`exam_session_programme_course` explicitly connects exams to curriculum entries.
`exam_venue` links exams to venues. Invigilators are separate staff accounts linked
through `invigilator_assignment` to an exam and venue; assigning a lecturer to a
course does not also assign them as an invigilator. Existing invigilator responses
resolve the course's lecturers through `course_lecturer`.

## Demo assignments

| Account | Assigned demo course |
| --- | --- |
| lecturer@gmail.com | CSC1101 — Foundations of Computing |
| lecturer2@gmail.com | CSC1202 — Programming Fundamentals |
| lecturer3@gmail.com | CSC2201 — Data Structures and Algorithms |
| lecturer4@gmail.com | CSC2302 — Database Systems |
| lecturer5@gmail.com | CSC3101 — Software Engineering Principles |

All five accounts use the existing shared demo password, `e1n2o3c4h5`.
The scripts can be rerun without duplicating accounts or assignments. Existing course
assignments are retained. A lecturer may have multiple explicitly assigned courses.

Use the signed-in lecturer's bearer token; do not send a staff ID to choose ownership.

`GET /api/exams/my-course-hierarchy` returns the lecturer's school, programme,
major, year, semester and course rows. A shared course appears once per curriculum
entry; group those rows for selectors rather than treating them as duplicate assignments.

1. `GET /api/exams/my-courses` returns assigned course codes in `data`.
2. `GET /api/exams` returns only exams for those courses. Administrators still see all exams.
3. `GET /api/exams/{examSessionId}/registered-students` and
   `GET /api/exams/{examSessionId}/venues` supply the allocation inputs.
4. `POST /api/allocation/exam-session/{examSessionId}` allocates registered students to venues.
5. `GET /api/allocation/exam-session/{examSessionId}` returns allocation statistics and venue assignments.
6. `GET /api/dashboard/lecturer` totals only assigned-course exams; the optional
   `examSessionId` parameter must also refer to an assigned course.

Lecturers cannot read another course's registrations or allocation statistics, or allocate
its students to venues: the API returns HTTP 403. Administrator read access and existing invigilator
venue access are retained. A lecturer with no course assignments sees empty exam/course
lists and zero dashboard totals. Registration totals count registrations per exam session.

An assignment alone does not create an examination, register students, or attach venues.
The existing examination, student-registration, and exam-venue records must match the
course, academic year, and semester. Allocation continues to use the existing venue capacity rules.

## After the programme-major migration

1. If the four extra demo lecturers are missing, run V28. It updates passwords
   and account fields for those demo accounts if rerun. The original demo lecturer
   must already exist with an active LECTURER account.
2. Run [Phase 16](../supabase/phase_16_link_lecturers_to_courses.sql). This is the
   post-V30 alternative to V29: it checks active lecturer roles and the BCS major
   hierarchy, then inserts the five course assignments in one transaction. It
   preserves existing assignments and passwords. No need to run both V29 and Phase 16.
3. Run [Phase 17](../supabase/phase_17_lecturer_allocation_readiness.sql). It reports
   exams, matching registration counts, linked venue capacity, curriculum mappings,
   and existing allocations. READY_FOR_ALLOCATION means these setup checks pass;
   it does not validate venue schedule conflicts or assign students to venues.
4. Sign in as the assigned lecturer and POST to the allocation endpoint above
   using an exam ID from the report. No request body is needed. GET the same URL
   to review venue assignments. Repeating POST replaces that exam's existing allocations.

Missing operational records require explicit student, exam and venue selections.
Do not run the old Phase 12 direct venue seed to test lecturer allocation: use the
API so its ownership/capacity checks run. A major is context for a shared course;
allocation currently includes all registrations for the exam's course, academic
year and semester, not just one selected major.

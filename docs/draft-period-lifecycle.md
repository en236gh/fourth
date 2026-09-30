# Examination Draft Period Lifecycle

## What Changed

Administrators can create multiple examination-period drafts with the same academic year, semester, and exam type. For example, several `2025/2026`, semester 1, `FINAL` drafts may coexist while you try different dates, course selections, or scheduling arrangements.

The restriction was enforced in two places: the period-creation service and a database unique constraint. The service restriction has been removed, and migration `V39__allow_multiple_draft_periods.sql` drops the old constraint for databases that already have the scheduling schema. Existing periods and their IDs are preserved.

The application has no frontend source in this repository. The backend API supports the behavior; a separate dashboard client must call the delete endpoint below to expose a delete action.

## Create a Draft

Use the existing administrator endpoint:

```http
POST /api/admin/examination-periods
Authorization: Bearer <administrator-token>
Content-Type: application/json
```

The request body is unchanged. The academic year is chosen from the current registration data by the service. The endpoint returns the full new period with revision `0`.

## Delete One Draft

Delete a specific period by ID, using the latest revision returned by `GET /api/admin/examination-periods` or `GET /api/admin/examination-periods/{id}`:

```http
DELETE /api/admin/examination-periods/12?revision=3
Authorization: Bearer <administrator-token>
```

A successful response uses the standard API envelope with message `Draft period deleted` and `data: null`. A stale revision is rejected; reload the period and confirm its current state before retrying.

Deletion is transactional and period-scoped. It removes that draft's draft invigilator assignments, generated allocations, venue bookings, generated exam sessions, course selection, and time slots, then deletes the period itself. Audit rows remain as history. It does not delete students, registrations, courses, venues, or another period, including another draft for the same cycle.

## Deletion Safeguards

Deletion is allowed only while the period status is `DRAFT`. It is refused if the period has published staffing, published exam sessions, completed or otherwise non-scheduled exams, attendance, incident records, generated reports, examination change requests, or amendment history. Published periods cannot be deleted.

Draft invigilator assignments are removed with the draft. If the period has operational records or reviewed/published history, the API returns an error and leaves the period in place. This is intentionally narrower than the development-only phase 25 reset script, which clears scheduling data across the database.

## Scheduling Within a Shared Cycle

Multiple drafts can share an academic year, semester, and exam type. Generation still blocks a selected course if an equivalent legacy exam or exam in a published period already exists. Another unpublished draft does not block that course solely because it shares the cycle.

Existing exams in other periods continue to count as timetable reservations: overlapping venue bookings and time placements remain unavailable. The cycle change does not bypass scheduling, capacity, registration, staffing, or publication validation.

## Database Deployment

Apply migration `V39__allow_multiple_draft_periods.sql` to the database before relying on multiple periods with the same cycle fields. This application has Flyway disabled, so adding the migration file does not apply it automatically; use the same deployment process used for the existing scheduling SQL migrations (for example, run the file once in Supabase SQL Editor). The migration drops only the old uniqueness constraint on `(academic_year, semester, exam_type)`; it does not alter existing period data or the per-period unique exam-course index.

## Tests

The opt-in PostgreSQL integration test `SchedulingPostgresTest` now verifies that two same-cycle drafts can be created and generated, deleting one leaves the other intact, a replacement draft can be created, stale revisions are rejected, and published periods cannot be deleted.

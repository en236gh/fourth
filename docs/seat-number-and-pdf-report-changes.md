# Seat-number removal and attendance PDF report changes

This guide describes the backend changes, their frontend impact, and the database
step required to use them.

## 1. Students are assigned to venues without numbered seats

Lecturers no longer assign students to venues. The assignment POST endpoint and
its automatic allocation logic have been removed. Existing student-to-venue records
remain in the database for attendance verification and examination passes.
Seat numbers are also removed; students have no numbered-seat labels.

Lecturer course ownership checks and read-only examination details remain available.

### API changes

The `seatNumber` field has been removed from:

- Venue allocation results and allocation detail items.
- Student examination summaries.
- Examination pass examination items.
- Attendance student lookup responses.

Examination pass PDFs also no longer display the Seat column. Student identifiers,
venue information, and the other existing fields remain available.

The allocation endpoints now behave as follows:

| Endpoint | Behavior |
| --- | --- |
| `POST /api/allocation/exam-session/{examSessionId}` | Removed; authenticated requests reaching this route receive HTTP 405. |
| `GET /api/allocation/exam-session/{examSessionId}` | Returns venue assignments and allocation statistics without seat numbers. |

The GET response retains the existing success, message, and data wrapper.
There is no replacement lecturer endpoint for creating venue assignments.

### Frontend changes

Remove seat-number columns, labels, form fields, and `seatNumber` properties from
frontend response types. Remove venue assignment buttons, dialogs, and POST calls. Keep read-only venue details. Update
student screens, attendance lookup screens, allocation tables, and examination
pass previews that previously displayed a seat number.

### Database migration

Apply [V31__remove_seat_numbers.sql](../src/main/resources/db/migration/V31__remove_seat_numbers.sql)
to the intended database after the earlier migrations and legacy demo seeds:

```sql
ALTER TABLE public.student_venue_allocation
    DROP COLUMN IF EXISTS seat_number;
```

This drops existing seat-number values while preserving student-to-venue assignment
rows. `IF EXISTS` allows the statement to be run again after the column is removed.

Flyway is disabled in the current configuration, so restarting the backend does
not apply this SQL automatically. The migration file being present in the project
does not mean it has been executed in your database.

Historical migrations and the database snapshot still contain the old column for
replaying the original setup. Run V31 after restoring that setup, and do not rerun
historical seat-number seeds after V31. The updated
[Phase 12 seed](../supabase/phase_12_seed_student_venue_allocations.sql) creates
venue assignments without seat numbers.

## 2. Lecturer PDF contains present students, absent students, and incidents

Previously, the lecturer report included attending students and incidents but
excluded absent students. The existing download now returns one PDF with three
sections:

| Section | Contents |
| --- | --- |
| Students in attendance | Computer number, name, programme, venue, check-in time, attendance status, and script submission indicator. |
| Absent students | Computer number, name, programme, and assigned venue. |
| Incident report | Occurrence time, type, severity, student where applicable, venue, reporter, and description. |

Each section includes its record count. Empty sections display a message instead
of an empty table. Table column headings repeat when a table continues onto another
page. No seat numbers appear in these lists.

### How attendance is classified

- `PRESENT`, `LATE`, and `WRONG_VENUE` records appear in the attending list.
- `ABSENT` records appear in the absent list.
- The PDF reads existing attendance records; downloading it does not mark students absent.

The existing end-examination process marks allocated students without attendance
records as absent. Complete the examination before downloading the final report.
A report downloaded earlier may have an incomplete absence list. Students who have
neither an attendance record nor a venue allocation are not automatically listed
as absent by this report.

### Download endpoint

```http
GET /api/reports/exam-session/{examSessionId}/pdf
Authorization: Bearer <accessToken>
```

The caller must have the `LECTURER` role and be assigned to the examination's
course. Use the examination session ID, not the course code. A lecturer requesting
another course's report is denied access.

The successful response is raw PDF bytes, not a JSON `data` object:

```http
Content-Type: application/pdf
Content-Disposition: attachment; filename="attendance-incidents-CS401-2026-08-23.pdf"
```

The filename uses the course code and examination date. Existing frontend download
code can keep the same URL and authenticated request. Handle a successful response
as a blob/file, and check unsuccessful responses before attempting to save a PDF.
One request downloads all three sections; separate present and absent downloads
are not required.

## 3. UNZA branding appears at the top

Attendance report PDFs display the UNZA crest and `UNIVERSITY OF ZAMBIA` above the
report content. The crest is bundled as
[`src/main/resources/images/unza-logo.png`](../src/main/resources/images/unza-logo.png),
so generating a report does not require an internet connection to fetch the logo.
The asset source is recorded in the [image README](../src/main/resources/images/README.md).

This branding change applies to lecturer attendance/incident reports and generated
invigilator attendance reports. The separate student examination pass PDF had its
Seat column removed; this change did not add branding to that pass.

## 4. Invigilator-generated attendance report

`POST /api/reports/exam-session/{examSessionId}` still returns generated-report
metadata in the existing JSON wrapper. It does not return the direct PDF download
response used by the lecturer endpoint.

The generated PDF now includes the UNZA header and attending/absent student lists
alongside attendance totals. Its records are limited to the venue selected by the
existing controller from the invigilator's assignments. The lecturer report covers
the selected examination across venues and includes incidents.

## 5. Verification

The five focused `ReportServiceTest` tests passed. They cover:

- Present, late, wrong-venue, and absent students in the appropriate sections,
  with incidents and an embedded logo.
- Clear messages when no attendance or incident records exist.
- A report containing 100 attending and 100 absent students across multiple pages.
- Invigilator report filtering to the selected venue.
- Rejection of a lecturer requesting another course's report.

Generated single-page and multi-page samples were rendered and visually checked.
These focused results do not claim that the full application's test suite passes.

### Manual acceptance checks

1. Apply V31 and restart the updated backend.
2. Confirm lecturers have no venue assignment controls, the removed POST is rejected, and existing assignment responses have no `seatNumber`.
3. Record attendance and incidents, then complete the examination.
4. Download the lecturer PDF and verify names, section counts, incidents, and logo.
5. Confirm absent students do not appear in the attending list.
6. Generate an invigilator report and confirm it only lists the selected venue's students.
7. Check student examination passes and frontend tables no longer show seat numbers.

For the wider lecturer API flow, see
[Lecturer frontend API changes](lecturer-frontend-api-changes.md).

## 6. Lecturer venue assignment removed

This later change removes the lecturer assignment action entirely. No new database
migration is needed for this change, and existing assignments are not deleted.
Read-only allocation statistics and dashboard totals still describe existing data.
Attendance, absence finalization, and pass generation continue to use venue
assignments; this change does not replace that operational dependency.

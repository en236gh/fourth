# Migrate the existing Supabase catalog to programmes and majors

Run each complete file in the Supabase SQL Editor, in the order below. This is
for the existing seeded database, not a fresh database installation. Stop if any
step fails. No live database was queried or changed while preparing this guide.

## Required order

1. [Preflight](../supabase/phase_14_programme_majors_preflight.sql) (read only).
   PostgreSQL must be 15+. The six legacy codes must exist in the expected schools.
   Save the curriculum IDs/count and exam-link count for comparison.
   `student_programme_enrolment` is optional; the other listed tables should exist
   for this application's academic and examination flows.
2. [V30: migrate programmes and majors](../src/main/resources/db/migration/V30__programme_majors.sql).
   Run the whole file including BEGIN and COMMIT. It already contains the required
   table creation, parent-programme and major insertions, curriculum updates,
   optional enrolment updates, constraints and index. No separate course inserts
   are needed. If the transaction fails, issue `ROLLBACK;` before retrying.
3. [Verify](../supabase/phase_15_verify_programme_majors.sql) (read only).
   Expect six majors, zero curriculum rows under legacy programmes, zero active
   legacy programmes, and unchanged curriculum IDs/count and exam-link count.

The resulting groupings are:

| School | Programme | Majors |
| --- | --- | --- |
| Natural Sciences (SNS) | Computer Science (BCS) | Computer Science; Software Engineering; Networking and Information Security |
| Engineering (ENG) | Engineering (BENG) | Electrical and Electronic; Civil and Environmental; Mechanical Engineering |

Education stays unchanged until its grouping is confirmed. Old programme rows
remain inactive for history. Existing programme-course IDs, exam mappings,
lecturer course assignments and venue allocations are preserved. Existing
normalized student enrolments gain their major; legacy `student.program` text
is retained. The migration does not infer or create missing student enrolments,
exam mappings or curricula.

## Optional lecturer demo data

V28 and V29 are not prerequisites for V30. If yesterday's lecturer seeds have
already been run, skip them. If you still need those demo accounts/assignments,
run the following after V30 and then rerun the verification file:

1. [V27 staff seed](../src/main/resources/db/migration/V27__seed_requested_staff_accounts.sql)
   only if the original demo lecturer is missing. This also seeds administrator
   and invigilator demo accounts and resets the listed existing accounts' values
   and password hashes, so it is not needed for the hierarchy migration.
2. [V28 additional lecturers](../src/main/resources/db/migration/V28__seed_additional_lecturers.sql)
   if the four additional demo lecturers are missing. This also resets those
   accounts' values/password hashes on conflict.
3. [V29 lecturer course links](../src/main/resources/db/migration/V29__seed_lecturer_course_assignments.sql).
   Requires all five demo lecturers and existing active CSC1101, CSC1202, CSC2201,
   CSC2302 and CSC3101 curriculum entries. It inserts missing assignment links
   without creating courses. Lecturer ownership remains per course, shared across
   its majors; it is not a separate assignment per major.

## Scripts not to rerun for this change

Do not replay all numbered migrations. Some older migrations reset data.
Do not rerun `phase_3_seed_academic_catalog.sql` after V30: it uses the old
programme layout and old uniqueness rule. Also do not rerun the old Phase 4
enrolment backfill or other legacy programme-based seeds after V30 without
updating their programme/major references first.

If preflight finds a missing catalog or missing legacy codes, stop: this guide
does not assume a fresh database or insert fabricated replacement curricula.
The existing Phase 3 course data is explicitly demo data.

Flyway is currently disabled in `application.properties`, so restarting the
backend does not run these scripts. There are also two V27 files in the migration
folder; resolve that version collision before enabling Flyway. Executing SQL in
the Supabase editor does not record a Flyway schema-history migration.

SQL changes alone do not deploy the backend or frontend. The frontend needs to
use the major-aware academic endpoints and active programme filters.

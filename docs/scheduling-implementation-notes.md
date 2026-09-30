# Scheduling implementation findings

The existing backend uses Spring Boot, JPA/JDBC and PostgreSQL. `exam_session`
identifies a course examination, `exam_venue` its booked venues, and
`student_venue_allocation` already has a student/exam primary key.

`AllocationService` reads allocations by exam ID, not by venue alone. The lecturer
frontend renders that response. The phase 12 development seed explicitly registers
seven computing students for each listed computing course. Stale allocations are
also counted without rechecking registration. These are confirmed code findings;
the reported live one-versus-seven case has not been verified against live data.

Reuse the existing staff roles, JWT authentication, course lecturer authorization,
academic catalogue, invigilator assignments and transaction advisory lock. Keep
legacy sessions published and outside new periods; do not infer historical period
ownership. New periods use explicit exam capacity, course registration eligibility,
bounded deterministic search, independent validation and explicit publication.

Frontend code is out of scope. See the final frontend integration guide for APIs,
migration instructions, examples and workflow diagrams.

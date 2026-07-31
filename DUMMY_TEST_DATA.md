# Dummy test data for the exam attendance system

This document describes a realistic seed dataset for testing the full exam-attendance flow in the application.

## Purpose

The data below is meant to let you test the system end to end as if a real university exam day were happening.

## What this seed data covers

- staff accounts for admin, invigilator, and lecturer roles
- students with realistic short dummy names
- university-style venues inspired by the University of Zambia
- exam sessions for different courses
- student-to-venue allocations

## Notes on realism

The venue names and departments are based on the University of Zambia style, including common campus facilities and school names such as:
- Great East Road Campus
- School of Engineering
- School of Education
- School of Natural Sciences
- Graduate School of Business

## Staff accounts

These accounts are created so authentication and role-based access work during testing.

| Email | Password | Role | Purpose |
|---|---|---|---|
| admin@unza.zm | Admin@2026 | ADMINISTRATOR | System administration |
| invigilator@unza.zm | Invig@2026 | INVIGILATOR | Main exam check-in |
| invigilator2@unza.zm | Invig2@2026 | INVIGILATOR | Secondary invigilator |
| lecturer@unza.zm | Lect@2026 | LECTURER | Lecturer access |

## Students

| Computer number | Name | Program | Year | Status |
|---|---|---|---|---|
| 2022004264 | K. Banda | Computer Science | 3 | ACTIVE |
| 2022004265 | T. Mwewa | Computer Science | 3 | ACTIVE |
| 2022004266 | L. Phiri | Computer Science | 3 | ACTIVE |
| 2022004267 | J. Sialumba | Electrical Engineering | 4 | ACTIVE |
| 2022004268 | M. Mulenga | Electrical Engineering | 4 | ACTIVE |
| 2022004269 | N. Chanda | Education | 2 | ACTIVE |
| 2022004270 | P. Mwaba | Education | 2 | ACTIVE |
| 2022004271 | S. Kalima | Business Administration | 3 | ACTIVE |

## Venues

| Venue | Building | Capacity |
|---|---|---:|
| Main LT 1 | Great East Road Campus | 250 |
| Engineering Lab 2 | School of Engineering | 120 |
| Education Block A | School of Education | 180 |
| Natural Science Hall | School of Natural Sciences | 200 |
| Business Hall | Graduate School of Business | 150 |

## Exam sessions

| Course | Date | Time | Academic year | Semester | Exam type |
|---|---|---|---|---|---|
| CS101 | 2026-08-10 | 09:00-11:00 | 2025/2026 | 1 | FINAL |
| EE221 | 2026-08-12 | 13:00-15:00 | 2025/2026 | 1 | FINAL |
| ED201 | 2026-08-14 | 10:00-12:00 | 2025/2026 | 1 | FINAL |

## Allocation examples

These records link students to exam sessions and venues.

| Student | Exam | Venue | Seat |
|---|---|---|---|
| 2022004264 | CS101 | Main LT 1 | A01 |
| 2022004265 | CS101 | Main LT 1 | A02 |
| 2022004267 | EE221 | Engineering Lab 2 | B01 |
| 2022004268 | EE221 | Engineering Lab 2 | B02 |
| 2022004269 | ED201 | Education Block A | C01 |
| 2022004270 | ED201 | Education Block A | C02 |

## Suggested test flow

1. Start the application and ensure PostgreSQL is running.
2. Apply the migrations so the new seed file is executed.
3. Log in with one of the staff accounts.
4. Use a student computer number and an exam session ID in the attendance check-in endpoint.
5. Confirm the attendance record is created.

## Example check-in payload

```json
{
  "computerNumber": "2022004264",
  "examSessionId": 1,
  "venueId": 1,
  "verificationMethod": "COMPUTER"
}
```

## Important note

The current implementation checks the student by computer number and creates an attendance record when the invigilator submits the request. The system is therefore ready for a manual, realistic test of the exam check-in experience.

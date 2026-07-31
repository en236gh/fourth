# Digital Examination Attendance System — Project Documentation

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture & Layering](#3-architecture--layering)
4. [Database Schema](#4-database-schema)
5. [Domain Entities & Relationships](#5-domain-entities--relationships)
6. [Business Logic by Module](#6-business-logic-by-module)
7. [API Endpoints](#7-api-endpoints)
8. [Security](#8-security)
9. [Configuration](#9-configuration)
10. [Database Migrations](#10-database-migrations)
11. [Testing](#11-testing)
12. [Known Limitations & Observations](#12-known-limitations--observations)

---

## 1. Project Overview

**Digital Examination Attendance System** is a Spring Boot 4.1.0 backend application designed to manage examination attendance at a university level. The system supports:

- **Student management** — storing student profiles, photos, QR tokens, and enrollment status.
- **Staff management** — staff profiles with role-based access control (Administrator, Lecturer, Invigilator).
- **Exam session management** — scheduling exams with course, date, time, academic year, semester, and exam type.
- **Venue management** — physical exam venues with capacity constraints.
- **Student-venue allocation** — automatically assigning students to venues for exam sessions.
- **Attendance tracking** — recording check-in events with verification methods (QR code, facial recognition, computer) and determining attendance status (Present, Absent, Late, Wrong Venue).
- **Incident reporting** — logging incidents during exams with severity levels (Minor, Major, Critical).
- **Report generation** — producing PDF attendance reports for exam sessions.
- **Dashboard** — role-based dashboards showing summary statistics.
- **Authentication & Authorization** — JWT-based stateless authentication with access and refresh tokens.

The application is named `fourth` and is part of the `com.backend.fourth` package namespace.

---

## 2. Technology Stack

| Layer | Technology |
|-------|-----------|
| **Java Version** | Java 21 |
| **Framework** | Spring Boot 4.1.0 |
| **Build Tool** | Maven (with `mvnw` wrapper) |
| **ORM** | Spring Data JPA (Hibernate) |
| **Database** | PostgreSQL 18 |
| **Database Migrations** | Flyway |
| **Security** | Spring Security 6, JWT (io.jsonwebtoken jjwt 0.12.6) |
| **Password Hashing** | BCrypt (strength 12) |
| **PDF Generation** | iText 5.5.13.3 |
| **Validation** | Jakarta Validation (spring-boot-starter-validation) |
| **JSON Processing** | Jackson (via spring-boot-starter-web) |
| **Lombok** | For boilerplate reduction (getters, setters, constructors) |
| **Testing** | JUnit 5, Mockito, Spring Boot Test |

### Maven Dependencies

| Dependency | Scope | Purpose |
|-----------|-------|---------|
| `spring-boot-starter-data-jpa` | compile | JPA/Hibernate ORM |
| `spring-boot-starter-flyway` | compile | Database migrations |
| `spring-boot-starter-web` | compile | REST API, embedded Tomcat |
| `spring-boot-starter-security` | compile | Authentication & authorization |
| `spring-boot-starter-validation` | compile | Bean validation |
| `flyway-database-postgresql` | compile | PostgreSQL Flyway support |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | compile/runtime | JWT token creation & parsing |
| `itextpdf` | compile | PDF report generation |
| `postgresql` | runtime | PostgreSQL JDBC driver |
| `lombok` | compile (optional) | Boilerplate reduction |
| `spring-boot-starter-test` | test | Testing framework |
| `spring-boot-devtools` | runtime (optional) | Development-time features |

---

## 3. Architecture & Layering

The application follows a standard layered architecture with clear separation of concerns:

```
com.backend.fourth
├── FourthApplication.java              # Main entry point
├── auth/                              # Authentication & JWT
│   ├── controller/AuthController.java
│   ├── dto/                           # LoginRequest, TokenResponse
│   ├── entity/RefreshToken.java
│   └── repository/RefreshTokenRepository.java
├── security/                          # JWT & Spring Security
│   ├── JwtAuthenticationFilter.java
│   ├── JwtConfiguration.java
│   ├── JwtProperties.java
│   ├── JwtService.java
│   └── SecurityConfig.java
├── staff/                             # Staff & Role management
│   ├── entity/Staff.java, Role.java
│   └── repository/StaffRepository.java
├── student/                           # Student management
│   ├── entity/Student.java
│   └── repository/StudentRepository.java
├── venue/                             # Venue management
│   ├── entity/Venue.java
│   └── repository/VenueRepository.java
├── exam/                              # Exam session management
│   ├── entity/ExamSession.java
│   └── repository/ExamSessionRepository.java
├── allocation/                        # Student-venue allocation
│   ├── controller/AllocationController.java
│   ├── entity/StudentVenueAllocation.java (+ composite ID)
│   ├── repository/StudentVenueAllocationRepository.java
│   └── service/AllocationService.java
├── attendance/                        # Attendance tracking
│   ├── controller/AttendanceController.java, IncidentController.java
│   ├── dto/CheckInRequest.java
│   ├── entity/Attendance.java, AttendanceStatus.java, VerificationMethod.java
│   ├── repository/AttendanceRepository.java
│   └── service/AttendanceService.java
├── report/                            # PDF report generation
│   ├── controller/ReportController.java
│   ├── entity/GeneratedReport.java
│   ├── repository/GeneratedReportRepository.java
│   └── service/ReportService.java
├── dashboard/                         # Role-based dashboards
│   └── controller/DashboardController.java
├── common/                            # Shared utilities
│   ├── ApiResponse.java
│   ├── config/JacksonConfig.java
│   └── exception/GlobalExceptionHandler.java
└── resources/
    ├── application.properties
    └── db/migration/                  # Flyway migrations V4, V5, V6
```

**Layer responsibilities:**

- **Controller layer** — Handles HTTP requests, performs input validation, resolves entity references from request parameters, delegates to service layer, and wraps responses in `ApiResponse`.
- **Service layer** — Contains business logic, transaction boundaries (`@Transactional`), and coordinates between repositories.
- **Repository layer** — Spring Data JPA interfaces extending `JpaRepository`, providing CRUD operations and custom query methods.
- **Entity layer** — JPA entities mapping to database tables, using Lombok for getters/setters.
- **Security layer** — JWT-based stateless authentication with a custom filter and Spring Security method-level authorization.

---

## 4. Database Schema

The application uses a PostgreSQL database named `exam_attendance`. The schema is managed via Flyway migrations and includes the following tables:

### Core Tables

| Table | Description |
|-------|-------------|
| `student` | Student profiles (computer number, national ID, name, program, year, contact, photo path, QR token, status) |
| `staff` | Staff profiles (staff number, name, email, phone, department, password hash) |
| `role` | Role definitions (ADMINISTRATOR, LECTURER, INVIGILATOR) |
| `staff_role` | Many-to-many join between staff and roles |
| `venue` | Exam venues (name, building, capacity) |
| `course` | Academic courses (code, name, credit hours, department) |
| `exam_session` | Scheduled exam sessions (course, date, start/end time, academic year, semester, exam type) |
| `exam_venue` | Many-to-many join between exam sessions and venues |
| `student_venue_allocation` | Student-to-venue assignments for exam sessions (with seat numbers) |
| `attendance` | Attendance records (student, exam session, venue, verified by staff, check-in time, verification method, status, scripts submitted, alert message) |
| `incident` | Incident reports during exams (exam session, venue, reported by staff, student, type, description, severity) |
| `incidence` | Additional incidence tracking table |
| `student_registration` | Student course registrations (computer number, course code, academic year, semester) |
| `course_lecturer` | Many-to-many join between courses and lecturers |
| `invigilator_assignment` | Invigilator assignments to exam sessions and venues |
| `generated_report` | Generated PDF report metadata (exam session, generated by, title, type, file path, summary) |
| `refresh_token` | JWT refresh tokens (staff, token, expiry, revoked, created at) |
| `flyway_schema_history` | Flyway migration tracking |

### Key Constraints

- **Student status** — Must be one of: `ACTIVE`, `SUSPENDED`, `GRADUATED`, `DEFERRED`
- **Student year of study** — Must be between 1 and 7
- **Attendance status** — Must be one of: `PRESENT`, `ABSENT`, `LATE`, `WRONG_VENUE`
- **Verification method** — Must be one of: `COMPUTER`, `QR_CODE`, `FACIAL_RECOGNITION`, `QR_AND_FACE`, `QR_AND_FACIAL`
- **Exam type** — Must be one of: `FINAL`, `SUPPLEMENTARY`, `SPECIAL`
- **Semester** — Must be 1 or 2
- **Exam session time** — `end_time` must be greater than `start_time`
- **Venue capacity** — Must be greater than 0
- **Staff role** — Must be one of: `INVIGILATOR`, `ADMINISTRATOR`, `LECTURER`
- **Incident severity** — Must be one of: `MINOR`, `MAJOR`, `CRITICAL`
- **Unique constraints** — Student email, national ID, staff email, staff number are all unique
- **Attendance uniqueness** — A student can only have one attendance record per exam session (unique constraint on `computer_number` + `exam_session_id`)

### Indexes

- `idx_attendance_session` — On `attendance(exam_session_id)`
- `idx_attendance_status` — On `attendance(attendance_status)`
- `idx_exam_session_course` — On `exam_session(course_code, academic_year, semester)`
- `idx_incidence_exam` / `idx_incidence_venue` — On `incidence(exam_session_id)` / `incidence(venue_id)`
- `idx_incident_session` — On `incident(exam_session_id)`
- `idx_registration_course` — On `student_registration(course_code, academic_year, semester)`
- `idx_generated_report_session` — On `generated_report(exam_session_id)`
- `idx_refresh_token_staff` — On `refresh_token(staff_id)`

---

## 5. Domain Entities & Relationships

### Student
- **Primary key:** `computerNumber` (String, e.g., "CS-2023-001")
- Stores personal and enrollment information including a `qrToken` used for QR-based verification.
- Status can be `ACTIVE`, `SUSPENDED`, `GRADUATED`, or `DEFERRED`.
- **Relationships:** One-to-many with `Attendance`, `StudentVenueAllocation`, `StudentRegistration`, `Incident` (via computer number).

### Staff
- **Primary key:** `staffId` (auto-generated integer)
- Stores staff profile and `passwordHash` (BCrypt encoded).
- **Relationships:**
  - Many-to-many with `Role` via `staff_role` join table (eagerly fetched).
  - One-to-many with `RefreshToken` (cascade all, orphan removal).
  - Referenced by `Attendance` (as `verifiedBy`), `Incident` (as `reportedBy`), `GeneratedReport` (as `generatedBy`).

### Role
- **Primary key:** `roleId` (auto-generated integer)
- Simple entity with a unique `name` field.
- Roles: `ADMINISTRATOR`, `LECTURER`, `INVIGILATOR`.

### Venue
- **Primary key:** `venueId` (auto-generated integer)
- Stores venue name, building, and capacity (must be > 0).
- **Relationships:** Referenced by `Attendance`, `StudentVenueAllocation`, `ExamVenue`, `Incident`, `InvigilatorAssignment`.

### ExamSession
- **Primary key:** `examSessionId` (auto-generated integer)
- Stores course code, exam date, start/end time, academic year, semester (1 or 2), and exam type (FINAL, SUPPLEMENTARY, SPECIAL).
- **Relationships:** Referenced by `Attendance`, `StudentVenueAllocation`, `ExamVenue`, `Incident`, `GeneratedReport`.

### StudentVenueAllocation (Composite Key)
- **Composite primary key:** `(computerNumber, examSessionId)` — implemented via `@IdClass(StudentVenueAllocationId.class)`.
- Stores the assigned `venueId` and `seatNumber` for each student in an exam session.
- **Relationships:** Foreign keys to `Student` and `ExamSession` (via `exam_venue` join).

### Attendance
- **Primary key:** `attendanceId` (auto-generated integer)
- Records a student's check-in for an exam session.
- **Relationships (all lazy):**
  - Many-to-one with `Student` (via `computer_number`)
  - Many-to-one with `ExamSession` (via `exam_session_id`)
  - Many-to-one with `Venue` (via `check_in_venue_id`)
  - Many-to-one with `Staff` (via `verified_by_staff_id`)
- Fields: `checkInTime`, `verificationMethod` (enum), `attendanceStatus` (enum), `scriptsSubmitted` (boolean, default false), `alertMessage` (text).

### GeneratedReport
- **Primary key:** `reportId` (auto-generated long)
- Stores metadata about generated PDF reports.
- **Relationships:** Many-to-one with `ExamSession`, many-to-one with `Staff` (as `generatedBy`).
- Fields: `title`, `reportType`, `filePath`, `generatedAt`, `summary`.

### RefreshToken
- **Primary key:** `tokenId` (auto-generated long)
- **Relationships:** Many-to-one with `Staff`.
- Fields: `token` (unique), `expiresAt`, `revoked` (boolean), `createdAt`.

---

## 6. Business Logic by Module

### 6.1 Authentication & Authorization

**Module:** `auth`, `security`

**Login Flow:**
1. Client sends a POST to `/api/auth/login` with email and password.
2. `AuthController.login()` looks up the staff by email. If not found, throws `IllegalArgumentException("Invalid credentials")`.
3. Password is verified using BCrypt (`PasswordEncoder.matches()`).
4. If password doesn't match, throws `IllegalArgumentException("Invalid credentials")`.
5. On success, `JwtService` generates:
   - **Access token** — valid for 5 minutes (300,000 ms), contains `subject` (email) and `roles` claim.
   - **Refresh token** — valid for 7 days (604,800,000 ms), contains `subject` (email) only.
6. The refresh token is persisted in the `refresh_token` table with `revoked=false` and `expiresAt = now + 7 days`.
7. Both tokens are returned in a `TokenResponse` DTO.

**JWT Details:**
- Secret key is loaded from `app.jwt.secret` property (default: `"change-this-to-a-strong-production-secret"`).
- Signing algorithm: HMAC-SHA256.
- Access token claims: `sub` (email), `roles` (list of role name strings), `iat`, `exp`.
- Refresh token claims: `sub` (email), `iat`, `exp`.

**Security Configuration (`SecurityConfig`):**
- CSRF is disabled.
- Session management is stateless (`SessionCreationPolicy.STATELESS`).
- `/api/auth/**` and `/actuator/health` are permitted without authentication.
- All other endpoints require authentication.
- `BCryptPasswordEncoder` with strength 12.
- Custom `UserDetailsService` loads staff by email, maps roles to authorities.
- `DaoAuthenticationProvider` configured with the custom user details service and password encoder.

**JWT Authentication Filter (`JwtAuthenticationFilter`):**
- Extends `OncePerRequestFilter`.
- Intercepts every request, extracts the `Authorization: Bearer <token>` header.
- Parses the JWT to extract username and roles.
- Creates a `UsernamePasswordAuthenticationToken` with authorities and sets it in the `SecurityContextHolder`.
- If token parsing fails, the exception is silently ignored (request proceeds without authentication, will be rejected by Spring Security if endpoint requires auth).

**Method-Level Authorization:**
- `@EnableMethodSecurity` enables `@PreAuthorize` annotations on controller methods.
- Roles used: `ADMINISTRATOR`, `LECTURER`, `INVIGILATOR`.

### 6.2 Staff & Role Management

**Module:** `staff`

**Staff Entity:**
- Staff members have a unique `staffNo`, `email`, `fullName`, `phone`, `department`, and `passwordHash`.
- Roles are managed via a many-to-many relationship with the `Role` entity through the `staff_role` join table.
- Roles are eagerly fetched (loaded immediately when staff is loaded).
- A staff member can have multiple roles (e.g., staff member 12 has both `INVIGILATOR` and `ADMINISTRATOR`).

**Role Entity:**
- Simple entity with `roleId` and `name`.
- Predefined roles: `ADMINISTRATOR`, `LECTURER`, `INVIGILATOR`.

**StaffRepository:**
- `findByEmail(String email)` — used for login and for looking up the invigilator/admin in controllers.
- `findByStaffNo(String staffNo)` — used for staff number lookups.

### 6.3 Student Management

**Module:** `student`

**Student Entity:**
- Primary key is `computerNumber` (String, e.g., "CS-2023-001").
- Fields: `nationalId` (unique), `fullName`, `program`, `yearOfStudy` (1-7), `email` (unique), `phone`, `photoPath`, `qrToken`, `status`.
- Status values: `ACTIVE`, `SUSPENDED`, `GRADUATED`, `DEFERRED`.

**StudentRepository:**
- `findByComputerNumber(String computerNumber)` — used in attendance check-in to resolve the student.

### 6.4 Venue Management

**Module:** `venue`

**Venue Entity:**
- Primary key is `venueId` (auto-generated integer).
- Fields: `venueName`, `building`, `capacity` (must be > 0).

**VenueRepository:**
- Standard `JpaRepository` with no custom methods.
- Used in allocation, attendance, and report modules.

### 6.5 Exam Session Management

**Module:** `exam`

**ExamSession Entity:**
- Primary key is `examSessionId` (auto-generated integer).
- Fields: `courseCode`, `examDate` (LocalDate), `startTime` (LocalTime), `endTime` (LocalTime), `academicYear` (String, e.g., "2025-2026"), `semester` (1 or 2), `examType` (FINAL, SUPPLEMENTARY, SPECIAL).
- Constraint: `endTime` must be after `startTime`.

**ExamSessionRepository:**
- Standard `JpaRepository` with no custom methods.
- Used in allocation, attendance, report, and dashboard modules.

### 6.6 Student-Venue Allocation

**Module:** `allocation`

**Business Logic (`AllocationService.allocateStudentsToVenues`):**
1. Retrieves all students from the database.
2. Retrieves all venues from the database.
3. Iterates through students, assigning each to a venue in a round-robin fashion.
4. For each student:
   - Creates a `StudentVenueAllocation` with the student's computer number, exam session ID, venue ID, and a seat number formatted as `"A" + (index + 1)` (e.g., "A1", "A2", ...).
   - Saves the allocation to the database.
   - Increments the index, wrapping around using modulo (`index = (index + 1) % venues.size()`).
5. **Stops** when the index reaches or exceeds the number of venues (i.e., only as many students as there are venues get allocated).
6. Returns the list of created allocations.

**AllocationController:**
- Endpoint: `POST /api/allocation/exam-session/{examSessionId}`
- Authorization: `hasAuthority('LECTURER')`
- Resolves the exam session by ID, then calls the allocation service.
- Returns a list of `StudentVenueAllocation` objects.

**Note:** The current allocation algorithm is simplistic — it assigns one student per venue in round-robin order and stops when all venues have been used once. It does not consider venue capacity, student course registration, or existing allocations.

### 6.7 Attendance Tracking

**Module:** `attendance`

**Check-In Flow (`AttendanceService.checkIn`):**
1. Checks for an existing attendance record for the same student and exam session. If found, throws `IllegalStateException("Student has already been checked in for this examination")` (HTTP 409 Conflict).
2. Creates a new `Attendance` entity with:
   - Student, exam session, venue, and verifying staff (invigilator).
   - `checkInTime` set to `LocalDateTime.now()`.
   - `verificationMethod` parsed from the request string.
   - `attendanceStatus` determined by `determineStatus()` (currently always returns `PRESENT`).
   - `scriptsSubmitted` set to `false`.
3. Saves and returns the attendance record.

**Verification Method Parsing (`parseVerification`):**
- Maps string input to `VerificationMethod` enum:
  - `"COMPUTER"` → `COMPUTER`
  - `"QR_CODE"` → `QR_CODE`
  - `"FACE_RECOGNITION"` → `FACIAL_RECOGNITION`
  - `"QR_AND_FACE"` → `QR_AND_FACE`
  - `"QR_AND_FACIAL"` → `QR_AND_FACIAL`
  - Any other value → throws `IllegalArgumentException("Unsupported verification method")`.

**Status Determination (`determineStatus`):**
- Currently a stub that always returns `AttendanceStatus.PRESENT`.
- The method signature accepts `Student`, `ExamSession`, `Venue`, and `CheckInRequest` but does not implement any logic.
- In a complete implementation, this would check:
  - Whether the student is at the correct venue (vs. `WRONG_VENUE`).
  - Whether the check-in time is past the exam start time (vs. `LATE`).
  - Whether the student is registered for the exam (vs. `ABSENT`).

**AttendanceController:**
- `POST /api/attendance/check-in` — Authorization: `INVIGILATOR` or `ADMINISTRATOR`. Accepts a `CheckInRequest` with `computerNumber`, `examSessionId`, `venueId`, and `verificationMethod`. Resolves student, exam session, venue, and invigilator (hardcoded email lookup for "invigilator@unza.zm").
- `GET /api/attendance/exam/{examSessionId}` — Authorization: `INVIGILATOR`, `ADMINISTRATOR`, or `LECTURER`. Returns all attendance records for an exam session.

**IncidentController:**
- `GET /api/incidents` — Authorization: `INVIGILATOR` or `ADMINISTRATOR`. Returns all attendance records where the status is not `PRESENT` (i.e., incidents: `ABSENT`, `LATE`, `WRONG_VENUE`).

**AttendanceRepository:**
- `findByStudentComputerNumberAndExamSessionExamSessionId(computerNumber, examSessionId)` — checks for duplicate attendance.
- `findByExamSessionExamSessionId(examSessionId)` — lists all attendance for an exam session.
- `countByExamSessionExamSessionIdAndAttendanceStatus(examSessionId, status)` — counts by status for an exam session.
- `countByAttendanceStatus(status)` — counts by status across all exams.

### 6.8 Report Generation

**Module:** `report`

**Business Logic (`ReportService.generateExamReport`):**
1. Creates a temporary PDF file using `Files.createTempFile("exam-report-", ".pdf")`.
2. Retrieves all attendance records for the given exam session.
3. Calls `writePdf()` to generate the PDF content.
4. Creates a `GeneratedReport` entity with:
   - Exam session, generated by staff, title ("Attendance Report - {examSessionId}"), report type ("EXAMINATION_ATTENDANCE"), file path, generated at timestamp, and summary ("Present={count}").
5. Saves and returns the report entity.

**PDF Content (`writePdf`):**
- Uses iText 5 (`com.itextpdf.text.Document`).
- Document contains:
  - Title: "Digital Examination Attendance Report"
  - Exam Session ID
  - Course code
  - Venue name
  - Counts for each status: Present, Absent, Late, Wrong Venue

**ReportController:**
- `POST /api/reports/exam-session/{examSessionId}` — Authorization: `ADMINISTRATOR` or `INVIGILATOR`. Generates a report for the specified exam session. Hardcodes venue lookup to ID 1 and staff lookup to email "admin@unza.zm".
- `GET /api/reports` — Authorization: `ADMINISTRATOR`. Lists all generated reports.

**GeneratedReportRepository:**
- Standard `JpaRepository` with no custom methods.

### 6.9 Dashboard

**Module:** `dashboard`

**DashboardController:**
- `GET /api/dashboard/admin` — Authorization: `ADMINISTRATOR`. Returns:
  - `totalExaminations` — count of all exam sessions.
  - `totalPresent` — count of attendance records with status `PRESENT`.
  - `totalAbsent` — count of attendance records with status `ABSENT`.
  - `totalReports` — count of all generated reports.
- `GET /api/dashboard/lecturer` — Authorization: `LECTURER`. Returns `assignedExaminations` (count of all exam sessions).
- `GET /api/dashboard/invigilator` — Authorization: `INVIGILATOR`. Returns `assignedExaminations` (count of all exam sessions).

**Note:** The lecturer and invigilator dashboards return the total count of all exam sessions rather than filtering by the specific staff member's assignments. This is a simplification.

### 6.10 Common Utilities

**Module:** `common`

**ApiResponse (record):**
- Generic wrapper for all API responses.
- Fields: `success` (boolean), `message` (String), `data` (T).
- Static factory methods:
  - `success(message, data)` — returns `ApiResponse(true, message, data)`.
  - `error(message)` — returns `ApiResponse(false, message, null)`.

**GlobalExceptionHandler:**
- `@RestControllerAdvice` that handles exceptions globally.
- `IllegalArgumentException` → HTTP 400 Bad Request.
- `IllegalStateException` → HTTP 409 Conflict.
- `MethodArgumentNotValidException` / `ConstraintViolationException` → HTTP 400 Bad Request ("Validation failed").
- `RuntimeException` → HTTP 500 Internal Server Error.

**JacksonConfig:**
- Provides a `Jackson2ObjectMapperBuilder` bean. Currently a no-op configuration (default builder).

---

## 7. API Endpoints

### Authentication

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/auth/login` | None (public) | Authenticate with email/password, returns access & refresh tokens |

### Allocation

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/allocation/exam-session/{examSessionId}` | `LECTURER` | Auto-allocate students to venues for an exam session |

### Attendance

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/attendance/check-in` | `INVIGILATOR`, `ADMINISTRATOR` | Record a student's check-in for an exam |
| GET | `/api/attendance/exam/{examSessionId}` | `INVIGILATOR`, `ADMINISTRATOR`, `LECTURER` | List all attendance records for an exam session |

### Incidents

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/incidents` | `INVIGILATOR`, `ADMINISTRATOR` | List all non-PRESENT attendance records (incidents) |

### Reports

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| POST | `/api/reports/exam-session/{examSessionId}` | `ADMINISTRATOR`, `INVIGILATOR` | Generate a PDF attendance report for an exam session |
| GET | `/api/reports` | `ADMINISTRATOR` | List all generated reports |

### Dashboard

| Method | Endpoint | Authorization | Description |
|--------|----------|---------------|-------------|
| GET | `/api/dashboard/admin` | `ADMINISTRATOR` | Admin dashboard with summary statistics |
| GET | `/api/dashboard/lecturer` | `LECTURER` | Lecturer dashboard |
| GET | `/api/dashboard/invigilator` | `INVIGILATOR` | Invigilator dashboard |

### Request/Response Formats

**Login Request:**
```json
{
  "email": "admin@unza.zm",
  "password": "password123"
}
```

**Login Response:**
```json
{
  "success": true,
  "message": "Authenticated",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**Check-In Request:**
```json
{
  "computerNumber": "CS-2023-001",
  "examSessionId": 4,
  "venueId": 1,
  "verificationMethod": "QR_CODE"
}
```

**Check-In Response:**
```json
{
  "success": true,
  "message": "Attendance recorded",
  "data": {
    "attendanceId": 1,
    "student": { "computerNumber": "CS-2023-001", ... },
    "examSession": { "examSessionId": 4, ... },
    "venue": { "venueId": 1, ... },
    "verifiedBy": { "staffId": 14, ... },
    "checkInTime": "2026-07-13T20:54:47.870034",
    "verificationMethod": "QR_CODE",
    "attendanceStatus": "PRESENT",
    "scriptsSubmitted": false,
    "alertMessage": null
  }
}
```

---

## 8. Security

### Authentication

- **Stateless JWT-based authentication** — no server-side sessions.
- **Access tokens** — 5-minute expiry, contain email (subject) and roles (claim).
- **Refresh tokens** — 7-day expiry, stored in the `refresh_token` table, can be revoked.
- **Password hashing** — BCrypt with strength 12.
- **Login endpoint** is public; all other endpoints require a valid JWT.

### Authorization

- **Method-level security** via `@PreAuthorize` annotations on controller methods.
- **Three roles:**
  - `ADMINISTRATOR` — Full access to all endpoints.
  - `LECTURER` — Can allocate students to venues, view attendance, view dashboard.
  - `INVIGILATOR` — Can check in students, view attendance, view incidents, generate reports, view dashboard.
- **Role-to-authority mapping:** Role names are used directly as Spring Security authorities (e.g., `hasAuthority('ADMINISTRATOR')`).

### JWT Filter

- `JwtAuthenticationFilter` extends `OncePerRequestFilter`.
- Extracts the `Authorization: Bearer <token>` header from every request.
- Parses the JWT using the configured secret key.
- Extracts username and roles from the token.
- Creates an `Authentication` object and sets it in the `SecurityContextHolder`.
- If token parsing fails, the exception is silently caught and the request proceeds (will be rejected if the endpoint requires authentication).

### Security Configuration

- CSRF protection is disabled (API-only, stateless).
- Session creation policy is `STATELESS`.
- Public endpoints: `/api/auth/**`, `/actuator/health`.
- All other endpoints require authentication.
- Custom `UserDetailsService` loads staff by email and maps roles to authorities.
- `DaoAuthenticationProvider` with BCrypt password encoder.

---

## 9. Configuration

### Application Properties (`application.properties`)

| Property | Value | Description |
|----------|-------|-------------|
| `spring.application.name` | `fourth` | Application name |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/exam_attendance` | PostgreSQL database URL |
| `spring.datasource.username` | `postgres` | Database username |
| `spring.datasource.password` | `e1n2o3c4h5` | Database password |
| `spring.datasource.driver-class-name` | `org.postgresql.Driver` | JDBC driver |
| `spring.jpa.hibernate.ddl-auto` | `none` | Hibernate does not manage schema (Flyway does) |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.PostgreSQLDialect` | PostgreSQL dialect |
| `spring.jpa.show-sql` | `true` | Log SQL queries |
| `spring.jpa.open-in-view` | `false` | Disable Open Session in View |
| `spring.flyway.enabled` | `true` | Enable Flyway migrations |
| `spring.flyway.locations` | `classpath:db/migration` | Migration script location |
| `spring.flyway.validate-on-migrate` | `false` | Skip validation on migrate |
| `spring.flyway.clean-disabled` | `true` | Disable clean operation |
| `app.jwt.secret` | `change-this-to-a-strong-production-secret` | JWT signing secret |
| `app.jwt.access-token-expiration-ms` | `300000` | Access token expiry (5 minutes) |
| `app.jwt.refresh-token-expiration-ms` | `604800000` | Refresh token expiry (7 days) |

### JWT Properties (`JwtProperties`)

- Bound to `app.jwt` prefix via `@ConfigurationProperties`.
- Default values: secret = `"dev-secret-key-change-me-in-production"`, access token expiry = 300,000 ms, refresh token expiry = 7 days.
- These defaults are overridden by `application.properties` values at runtime.

---

## 10. Database Migrations

The project uses Flyway for database schema management. Migrations are located in `src/main/resources/db/migration/`.

### V4 — Attendance Domain Alignment (`V4__attendance_domain_alignment.sql`)

1. Updates the `verification_method` check constraint on `attendance_record` to include `COMPUTER` and `QR_AND_FACE` (in addition to `QR_CODE`, `FACIAL_RECOGNITION`, `QR_AND_FACIAL`).
2. Creates the `role` table with `role_id` (SERIAL) and `name` (unique).
3. Inserts three roles: `ADMINISTRATOR`, `LECTURER`, `INVIGILATOR`.
4. Creates the `refresh_token` table with foreign key to `staff`.
5. Creates an index on `refresh_token(staff_id)`.
6. Creates the `generated_report` table with foreign keys to `exam_session` and `staff`, and a `summary` JSONB column.
7. Creates an index on `generated_report(exam_session_id)`.

### V5 — Allow Existing Flyway History (`V5__allow_existing_flyway_history.sql`)

- Inserts a row into `flyway_schema_history` for version 4 to avoid Flyway validation failure when the schema already contains earlier baseline migrations (versions 1, 2, 3 from the original database dump).
- Uses `ON CONFLICT (installed_rank) DO NOTHING` to be idempotent.

### V6 — Final Attendance Schema (`V6__final_attendance_schema.sql`)

1. **Renames tables:**
   - `attendance_record` → `attendance`
   - `incident_report` → `incident`
2. **Creates `role` table** (if not exists) and populates it from existing `staff_role` data.
3. **Rebuilds `staff_role` table:**
   - Creates `staff_role_new` with `staff_id` and `role_id` (referencing the new `role` table).
   - Migrates data from the old `staff_role` (which stored role names as strings) to the new table (using role IDs).
   - Drops the old `staff_role` and renames `staff_role_new` to `staff_role`.
4. **Updates `attendance` table:**
   - Updates the verification method check constraint.
   - Adds `attendance_status` column (VARCHAR(30)) with default `PRESENT`.
   - Backfills `attendance_status` from the old `status` column.
   - Adds a check constraint for valid status values.
   - Creates indexes on `exam_session_id` and `attendance_status`.
5. **Updates `incident` table:**
   - Adds `incident_type`, `evidence_path`, and `occurred_at` columns.
   - Backfills `incident_type` (default `OTHER`) and `occurred_at` (from `incident_time` or current timestamp).
   - Sets `occurred_at` as NOT NULL, `description` as NOT NULL, and `severity` default to `MINOR`.
6. **Updates `generated_report` table:**
   - Adds `generated_by_staff_id` column with foreign key to `staff`.
   - Creates an index on `exam_session_id`.

### Original Migrations (from database dump)

The database dump (`exam_attendance.sql`) shows three earlier migrations in the `flyway_schema_history`:

| Version | Description |
|---------|-------------|
| 1 | Flyway Baseline |
| 2 | Add alert message to attendance record |
| 3 | Create incidence table |

These were applied to the database before the V4-V6 migrations were added to the codebase.

---

## 11. Testing

### Unit Tests

**AttendanceServiceTest** (`src/test/java/com/backend/fourth/attendance/AttendanceServiceTest.java`)

- Uses Mockito with `@ExtendWith(MockitoExtension.class)`.
- **Test:** `shouldRejectDuplicateAttendanceForSameStudentAndExamSession`
  - Mocks `AttendanceRepository` to return an existing `Attendance` when `findByStudentComputerNumberAndExamSessionExamSessionId` is called.
  - Calls `attendanceService.checkIn()` with a `CheckInRequest` for the same student and exam session.
  - Asserts that `IllegalStateException` is thrown (duplicate check-in prevention).
- Helper methods create mock `Staff`, `Student`, `ExamSession`, and `Venue` objects.

**StaffRoleMappingTest** (`src/test/java/com/backend/fourth/staff/entity/StaffRoleMappingTest.java`)

- **Test:** `staffShouldExposeRoleNamesFromTheRoleEntity`
  - Creates a `Staff` with a single `Role` named "ADMIN".
  - Verifies that `staff.getRoles().stream().map(Role::getName)` returns a set containing "ADMIN".
  - This test validates the many-to-many relationship mapping between `Staff` and `Role`.

**FourthApplicationTests** (`src/test/java/com/backend/fourth/FourthApplicationTests.java`)

- Standard Spring Boot test that verifies the application context loads successfully.
- `@SpringBootTest` annotation.
- `contextLoads()` test method (empty body, just verifies context initialization).

---

## 12. Known Limitations & Observations

1. **Allocation algorithm is simplistic** — `AllocationService.allocateStudentsToVenues()` assigns one student per venue in round-robin order and stops when all venues have been used once. It does not consider:
   - Venue capacity.
   - Student course registration.
   - Existing allocations (no deduplication).
   - Seat number uniqueness within a venue.

2. **Status determination is a stub** — `AttendanceService.determineStatus()` always returns `PRESENT`. The logic to detect `LATE`, `WRONG_VENUE`, or `ABSENT` is not implemented.

3. **Hardcoded lookups in controllers:**
   - `AttendanceController.checkIn()` hardcodes the invigilator email as `"invigilator@unza.zm"`.
   - `ReportController.generateReport()` hardcodes venue ID as `1` and staff email as `"admin@unza.zm"`.

4. **Dashboard limitations:**
   - Lecturer and invigilator dashboards return the total count of all exam sessions, not filtered by the authenticated user's assignments.
   - No filtering by academic year, semester, or course.

5. **No refresh token usage** — The `RefreshToken` entity and `RefreshTokenRepository` exist, but there is no endpoint to refresh an access token using a refresh token.

6. **No student registration data** — The `student_registration` table exists in the database but has no corresponding JPA entity or repository in the codebase.

7. **No course or course_lecturer entities** — The `course` and `course_lecturer` tables exist in the database but have no corresponding JPA entities.

8. **No invigilator assignment entity** — The `invigilator_assignment` table exists in the database but has no corresponding JPA entity.

9. **No incidence entity** — The `incidence` table exists in the database but has no corresponding JPA entity.

10. **No CRUD endpoints for core entities** — There are no REST endpoints for creating, reading, updating, or deleting students, staff, venues, exam sessions, or courses. The API only supports attendance check-in, allocation, reporting, and dashboard views.

11. **No student-facing authentication** — Students cannot log in; the system only supports staff authentication.

12. **PDF files are written to temporary files** — Generated reports are stored as temp files with no cleanup mechanism.

13. **JWT secret is hardcoded in properties** — The default secret `"change-this-to-a-strong-production-secret"` should be replaced in production.

14. **No rate limiting** — No protection against brute-force attacks on the login endpoint.

15. **Exception handling is broad** — `RuntimeException` is caught as a catch-all and returns HTTP 500, which may mask unexpected errors.

---

*Generated from source code analysis of the Digital Examination Attendance System (`fourth` Spring Boot application).*

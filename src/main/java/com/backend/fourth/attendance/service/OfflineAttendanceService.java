package com.backend.fourth.attendance.service;

import com.backend.fourth.attendance.dto.CheckInRequest;
import com.backend.fourth.staff.entity.Staff;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.*;
import java.util.*;

@Service
public class OfflineAttendanceService {
    private final JdbcTemplate jdbc;
    private final AttendanceService attendance;
    private final PlatformTransactionManager transactions;

    public OfflineAttendanceService(JdbcTemplate jdbc, AttendanceService attendance,
                                    PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.attendance = attendance;
        this.transactions = transactions;
    }

    public record Snapshot(UUID snapshotId, int staffId, String generatedAt,
                           List<Map<String, Object>> assignments, List<Map<String, Object>> students) {}
    public record Result(String scanId, String outcome, String reason, String message,
                         Integer attendanceId, String processedAt) {}

    public Snapshot download(Staff actor) {
        TransactionTemplate tx = transaction();
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        return tx.execute(status -> {
            UUID id = UUID.randomUUID();
            String generatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).toString();
            jdbc.update("INSERT INTO attendance_offline_snapshot(snapshot_id,staff_id,generated_at) VALUES (?,?,?::timestamptz)",
                    id, actor.getStaffId(), generatedAt);
            List<Map<String, Object>> assignments = jdbc.queryForList("""
                    SELECT DISTINCT e.exam_session_id AS "examSessionId", e.course_code AS "courseCode",
                        e.exam_date::text AS "examDate", e.start_time::text AS "startTime", e.end_time::text AS "endTime",
                        e.academic_year AS "academicYear", e.semester, e.exam_type AS "examType", e.status,
                        v.venue_id AS "venueId", v.venue_name AS "venueName"
                    FROM invigilator_assignment a JOIN exam_session e USING(exam_session_id)
                    JOIN venue v ON v.venue_id=a.venue_id
                    WHERE a.staff_id=? AND a.assignment_status='PUBLISHED' AND e.schedule_published
                    ORDER BY e.exam_session_id,v.venue_id
                    """, actor.getStaffId());
            jdbc.update("""
                    INSERT INTO attendance_offline_roster(snapshot_id,exam_session_id,venue_id,computer_number)
                    SELECT DISTINCT ?,r.exam_session_id,r.venue_id,r.computer_number
                    FROM student_venue_allocation r JOIN exam_session e USING(exam_session_id)
                    JOIN invigilator_assignment a ON a.exam_session_id=r.exam_session_id AND a.venue_id=r.venue_id
                    JOIN student_registration reg ON reg.computer_number=r.computer_number
                        AND reg.course_code=e.course_code AND reg.academic_year=e.academic_year AND reg.semester=e.semester
                    WHERE a.staff_id=? AND a.assignment_status='PUBLISHED' AND e.schedule_published
                    """, id, actor.getStaffId());
            List<Map<String, Object>> students = jdbc.queryForList("""
                    SELECT r.exam_session_id AS "examSessionId",r.venue_id AS "venueId",
                        s.computer_number AS "computerNumber",s.full_name AS "fullName",
                        s.program,s.photo_path AS "photoPath",att.attendance_status AS "attendanceStatus"
                    FROM attendance_offline_roster r JOIN student s USING(computer_number)
                    LEFT JOIN attendance att ON att.computer_number=r.computer_number AND att.exam_session_id=r.exam_session_id
                    WHERE r.snapshot_id=? ORDER BY r.exam_session_id,r.venue_id,s.computer_number
                    """, id);
            return new Snapshot(id, actor.getStaffId(), generatedAt, assignments, students);
        });
    }

    /** Each item commits independently. Infrastructure failures propagate: clients safely retry UUIDs. */
    public List<Result> sync(List<Object> scans, Staff actor) {
        if (scans == null || scans.isEmpty() || scans.size() > 200)
            throw new IllegalArgumentException("scans must contain between 1 and 200 items");
        List<Result> results = new ArrayList<>();
        for (Object item : scans) {
            Map<?, ?> input = item instanceof Map<?, ?> map ? map : Map.of();
            UUID id;
            try { id = uuid(input, "scanId"); }
            catch (IllegalArgumentException invalid) {
                results.add(rejected(null, "INVALID_SCAN_ID", "Each scan requires a canonical UUID scanId"));
                continue;
            }
            results.add(transaction().execute(status -> process(id, input, actor)));
        }
        return results;
    }

    private TransactionTemplate transaction() {
        TransactionTemplate tx = new TransactionTemplate(transactions);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return tx;
    }

    private Result process(UUID id, Map<?, ?> input, Staff actor) {
        // Serializes retries across instances; a hash collision only adds harmless contention.
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, id.toString());
        var previous = jdbc.queryForList("SELECT * FROM attendance_sync_scan WHERE scan_id=?", id);
        if (!previous.isEmpty()) {
            var row = previous.getFirst();
            if (!Objects.equals(row.get("staff_id"), actor.getStaffId()))
                return rejected(id, "SCAN_ID_OWNED_BY_ANOTHER_USER", "Scan ID cannot be used by this account");
            return jdbc.queryForObject("""
                    SELECT * FROM attendance_sync_scan WHERE scan_id=?
                    """, (rs, n) -> new Result(id.toString(), rs.getString("outcome"), rs.getString("reason"),
                    rs.getString("message"), rs.getObject("attendance_id", Integer.class),
                    rs.getObject("processed_at", OffsetDateTime.class).toInstant().toString()), id);
        }
        Result result;
        try { result = validateAndInsert(id, input, actor); }
        catch (Rejection ex) { result = rejected(id, ex.code, ex.getMessage()); }
        catch (IllegalArgumentException | IllegalStateException ex) {
            result = rejected(id, ruleCode(ex.getMessage()), ex.getMessage());
        }
        jdbc.update("""
                INSERT INTO attendance_sync_scan(scan_id,staff_id,outcome,reason,message,attendance_id,processed_at)
                VALUES (?,?,?,?,?,?,?::timestamptz)
                """, id, actor.getStaffId(), result.outcome(), result.reason(), result.message(),
                result.attendanceId(), result.processedAt());
        return result;
    }

    private Result validateAndInsert(UUID id, Map<?, ?> input, Staff actor) {
        UUID snapshot = uuid(input, "snapshotId");
        int exam = integer(input, "examSessionId");
        int venue = integer(input, "venueId");
        String method = required(input, "verificationMethod").toUpperCase(Locale.ROOT);
        Instant captured;
        try { captured = OffsetDateTime.parse(required(input, "capturedAt")).toInstant(); }
        catch (DateTimeException ex) { throw new Rejection("INVALID_CAPTURE_TIME", "capturedAt must include a UTC offset"); }
        if (captured.isAfter(Instant.now()))
            throw new Rejection("INVALID_CAPTURE_TIME", "Captured time cannot be in the future");
        var snapshots = jdbc.query("SELECT generated_at FROM attendance_offline_snapshot WHERE snapshot_id=? AND staff_id=?",
                (rs, n) -> rs.getObject(1, OffsetDateTime.class).toInstant(), snapshot, actor.getStaffId());
        if (snapshots.isEmpty()) throw new Rejection("INVALID_SNAPSHOT", "Snapshot is missing or belongs to another account");
        if (captured.isBefore(snapshots.getFirst()))
            throw new Rejection("INVALID_CAPTURE_TIME", "Captured time precedes the downloaded snapshot");

        // Hold current authorization and exam state stable until the attendance/outcome commit.
        jdbc.query("SELECT exam_session_id FROM exam_session WHERE exam_session_id=? FOR SHARE", rs -> {}, exam);
        jdbc.query("""
                SELECT staff_id FROM invigilator_assignment
                WHERE exam_session_id=? AND venue_id=? AND staff_id=? FOR SHARE
                """, rs -> {}, exam, venue, actor.getStaffId());
        String student = optional(input, "computerNumber");
        String token = optional(input, "qrToken");
        if (method.startsWith("QR") && token == null)
            throw new Rejection("INVALID_QR", "QR verification requires the captured examination-pass token");
        if (token != null) {
            String resolved;
            try { resolved = attendance.resolveComputerNumberFromQr(token, exam); }
            catch (IllegalArgumentException ex) { throw new Rejection("INVALID_QR", ex.getMessage()); }
            if (student != null && !student.equals(resolved))
                throw new Rejection("IDENTITY_MISMATCH", "Student number does not match the QR token");
            student = resolved;
        }
        if (student == null || !student.matches("\\d{10}"))
            throw new Rejection("INVALID_STUDENT_NUMBER", "A 10-digit computerNumber or valid QR token is required");
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM attendance_offline_roster
                WHERE snapshot_id=? AND exam_session_id=? AND venue_id=? AND computer_number=?)
                """, Boolean.class, snapshot, exam, venue, student)))
            throw new Rejection("NOT_IN_SNAPSHOT", "Student was not on this downloaded examination/venue roster");
        jdbc.query("SELECT computer_number FROM student_venue_allocation WHERE exam_session_id=? AND computer_number=? FOR SHARE",
                rs -> {}, exam, student);
        // Eligibility rows are also protected against deletion during authoritative validation.
        jdbc.query("""
                SELECT r.computer_number FROM student_registration r JOIN exam_session e
                ON r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester
                WHERE e.exam_session_id=? AND r.computer_number=? FOR SHARE OF r
                """, rs -> {}, exam, student);
        var prepared = attendance.prepareCheckIn(new CheckInRequest(student, exam, venue, method), actor,
                LocalDateTime.ofInstant(captured, ZoneId.systemDefault()));
        String processedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).toString();
        var inserted = jdbc.query("""
                INSERT INTO attendance(computer_number,exam_session_id,check_in_venue_id,verified_by_staff_id,
                    check_in_time,verification_method,attendance_status,scripts_submitted,client_scan_id,processed_at)
                VALUES (?,?,?,?,?,?,'PRESENT',false,?,?::timestamptz)
                ON CONFLICT (computer_number,exam_session_id) DO NOTHING RETURNING attendance_id
                """, (rs, n) -> rs.getInt(1), student, exam, venue, actor.getStaffId(),
                prepared.getCheckInTime(), prepared.getVerificationMethod().name(), id, processedAt);
        if (!inserted.isEmpty())
            return new Result(id.toString(), "ACCEPTED", "RECORDED", "Attendance recorded", inserted.getFirst(), processedAt);
        var existing = jdbc.queryForMap("SELECT attendance_id,attendance_status FROM attendance WHERE computer_number=? AND exam_session_id=?", student, exam);
        if ("ABSENT".equals(existing.get("attendance_status")))
            throw new Rejection("ATTENDANCE_CONFLICT", "Student is already marked absent; attendance was not changed");
        return new Result(id.toString(), "ALREADY_RECORDED", "ALREADY_RECORDED", "Attendance already exists; original record preserved",
                (Integer) existing.get("attendance_id"), processedAt);
    }

    private static String ruleCode(String message) {
        return switch (message) {
            case "You are not assigned to this examination venue" -> "NOT_ASSIGNED";
            case "Examination timetable is not published" -> "EXAM_NOT_PUBLISHED";
            case "Examination has already been completed" -> "EXAM_COMPLETED";
            case "Student is not registered for this course examination" -> "NOT_ELIGIBLE";
            case "Student is not allocated to this examination", "Student must check in at their allocated examination venue" -> "ALLOCATION_CHANGED";
            case "Unsupported verification method" -> "INVALID_VERIFICATION_METHOD";
            default -> "INVALID_SCAN";
        };
    }
    private static Result rejected(UUID id, String code, String message) {
        return new Result(id == null ? null : id.toString(), "REJECTED", code, message, null, Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS).toString());
    }
    private static String optional(Map<?, ?> input, String field) {
        Object value = input.get(field);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new IllegalArgumentException(field + " must be a string");
        return text.isBlank() ? null : text.trim();
    }
    private static String required(Map<?, ?> input, String field) {
        String value = optional(input, field);
        if (value == null) throw new IllegalArgumentException(field + " is required");
        return value;
    }
    private static UUID uuid(Map<?, ?> input, String field) {
        String value = required(input, field);
        UUID id = UUID.fromString(value);
        if (!id.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException(field + " must be a canonical UUID");
        return id;
    }
    private static int integer(Map<?, ?> input, String field) {
        Object value = input.get(field);
        if (!(value instanceof Integer number) || number <= 0)
            throw new IllegalArgumentException(field + " must be a positive integer");
        return number;
    }
    private static class Rejection extends IllegalArgumentException {
        private final String code;
        Rejection(String code, String message) { super(message); this.code = code; }
    }
}

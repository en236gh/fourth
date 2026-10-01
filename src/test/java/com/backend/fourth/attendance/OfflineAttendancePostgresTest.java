package com.backend.fourth.attendance;

import com.backend.fourth.attendance.service.*;
import com.backend.fourth.attendance.repository.AttendanceRepository;
import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.*;
import com.backend.fourth.student.service.ExamPassQrService;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Opt-in, disposable database only. See docs/offline-attendance-sync.md. */
@EnabledIfSystemProperty(named="attendance.it", matches="true")
class OfflineAttendancePostgresTest {
    private static final String ROOT = "jdbc:postgresql://127.0.0.1:55440/";
    private String database;
    private JdbcTemplate jdbc;
    private OfflineAttendanceService service;
    private ExamPassQrService qr;
    private Staff actor;
    private OfflineAttendanceService.Snapshot snapshot;

    @BeforeEach void setup() throws Exception {
        database = "attendance_it_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(ROOT + "postgres", System.getProperty("user.name"), "");
             var statement = connection.createStatement()) { statement.execute("CREATE DATABASE " + database); }
        var ds = new DriverManagerDataSource(ROOT + database, System.getProperty("user.name"), "");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
                CREATE TABLE staff(staff_id integer PRIMARY KEY);
                CREATE TABLE exam_session(exam_session_id integer PRIMARY KEY,course_code text,exam_date date DEFAULT CURRENT_DATE,
                    start_time time DEFAULT '09:00',end_time time DEFAULT '12:00',academic_year text DEFAULT '2026',
                    semester integer DEFAULT 1,exam_type text DEFAULT 'FINAL',status text DEFAULT 'SCHEDULED',schedule_published boolean DEFAULT true);
                CREATE TABLE venue(venue_id integer PRIMARY KEY,venue_name text);
                CREATE TABLE invigilator_assignment(exam_session_id integer,venue_id integer,staff_id integer,assignment_status text DEFAULT 'PUBLISHED');
                CREATE TABLE student(computer_number text PRIMARY KEY,full_name text,program text,photo_path text);
                CREATE TABLE student_registration(computer_number text,course_code text,academic_year text DEFAULT '2026',semester integer DEFAULT 1);
                CREATE TABLE student_venue_allocation(computer_number text,exam_session_id integer,venue_id integer);
                CREATE TABLE attendance(attendance_id serial PRIMARY KEY,computer_number text,exam_session_id integer,
                    check_in_venue_id integer,verified_by_staff_id integer,check_in_time timestamp,verification_method text,
                    attendance_status text,scripts_submitted boolean,UNIQUE(computer_number,exam_session_id));
                INSERT INTO staff VALUES (1),(2);
                INSERT INTO venue VALUES (1,'One'),(2,'Two');
                INSERT INTO exam_session(exam_session_id,course_code) VALUES (1,'CS'),(2,'MA');
                INSERT INTO exam_session(exam_session_id,course_code,schedule_published) VALUES (3,'CS',false);
                INSERT INTO invigilator_assignment VALUES (1,1,1,'PUBLISHED'),(1,2,2,'PUBLISHED'),(2,1,1,'DRAFT'),(3,1,1,'PUBLISHED');
                INSERT INTO student VALUES ('2022000001','First','CS','/photo1'),('2022000002','Second','CS','/photo2');
                INSERT INTO student_registration(computer_number,course_code) VALUES ('2022000001','CS'),('2022000002','CS');
                INSERT INTO student_venue_allocation VALUES ('2022000001',1,1),('2022000002',1,2),('2022000001',3,1);
                """);
        try (var in = getClass().getResourceAsStream("/db/migration/V40__offline_attendance_sync.sql")) {
            jdbc.execute(new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8));
        }
        // Repository adapters read real mutable DB state; validation is the production AttendanceService.
        var students = mock(StudentRepository.class);
        when(students.findByComputerNumber(anyString())).thenAnswer(i -> {
            var found = jdbc.query("SELECT computer_number FROM student WHERE computer_number=?", (rs,n) -> {
                var s = new Student(); s.setComputerNumber(rs.getString(1)); return s;
            }, i.getArgument(0, Object.class)); return found.stream().findFirst();
        });
        var exams = mock(ExamSessionRepository.class);
        when(exams.findById(anyInt())).thenAnswer(i -> jdbc.query("SELECT * FROM exam_session WHERE exam_session_id=?", (rs,n) -> {
            var e = new ExamSession(); e.setExamSessionId(rs.getInt("exam_session_id")); e.setCourseCode(rs.getString("course_code"));
            e.setStatus(rs.getString("status")); e.setSchedulePublished(rs.getBoolean("schedule_published"));
            e.setAcademicYear(rs.getString("academic_year")); e.setSemester(rs.getInt("semester")); return e;
        }, i.getArgument(0, Object.class)).stream().findFirst());
        var venues = mock(VenueRepository.class);
        when(venues.findById(anyInt())).thenAnswer(i -> {var v=new Venue();v.setVenueId(i.getArgument(0));return Optional.of(v);});
        var allocations = mock(StudentVenueAllocationRepository.class);
        when(allocations.findByComputerNumberAndExamSessionId(anyString(),anyInt())).thenAnswer(i -> jdbc.query(
                "SELECT * FROM student_venue_allocation WHERE computer_number=? AND exam_session_id=?", (rs,n) -> {
                    var a=new StudentVenueAllocation(); a.setComputerNumber(rs.getString(1));a.setExamSessionId(rs.getInt(2));a.setVenueId(rs.getInt(3));return a;
                }, i.getArgument(0),i.getArgument(1)).stream().findFirst());
        var assignments = mock(InvigilatorAssignmentRepository.class);
        when(assignments.existsByExamSessionIdAndVenueIdAndStaffIdAndAssignmentStatus(anyInt(),anyInt(),anyInt(),anyString()))
                .thenAnswer(i -> jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM invigilator_assignment WHERE exam_session_id=? AND venue_id=? AND staff_id=? AND assignment_status=?)",
                        Boolean.class,i.getArgument(0),i.getArgument(1),i.getArgument(2),i.getArgument(3)));
        var registrations = mock(StudentRegistrationRepository.class);
        when(registrations.existsByComputerNumberAndCourseCodeAndAcademicYearAndSemester(anyString(),anyString(),anyString(),anyInt()))
                .thenAnswer(i -> jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM student_registration WHERE computer_number=? AND course_code=? AND academic_year=? AND semester=?)",
                        Boolean.class,i.getArgument(0),i.getArgument(1),i.getArgument(2),i.getArgument(3)));
        qr=mock(ExamPassQrService.class);
        var attendance = new AttendanceService(mock(AttendanceRepository.class), students, exams, venues, allocations, assignments,
                mock(StaffRepository.class),mock(ExaminationPassRepository.class),qr,registrations,
                mock(com.backend.fourth.exam.service.LecturerCourseAccess.class),mock(com.backend.fourth.common.security.CurrentStaffResolver.class));
        service = new OfflineAttendanceService(jdbc,attendance,new DataSourceTransactionManager(ds));
        actor=new Staff(); actor.setStaffId(1);
        snapshot=service.download(actor);
    }

    @AfterEach void cleanup() throws Exception {
        if (database!=null) try(var c=DriverManager.getConnection(ROOT+"postgres",System.getProperty("user.name"),"");var s=c.createStatement()) {
            s.execute("DROP DATABASE "+database+" WITH (FORCE)");
        }
    }
    private Map<String,Object> scan() {
        return new HashMap<>(Map.of("scanId",UUID.randomUUID().toString(),"snapshotId",snapshot.snapshotId().toString(),
                "examSessionId",1,"venueId",1,"computerNumber","2022000001","verificationMethod","COMPUTER","capturedAt",Instant.now().toString()));
    }
    private OfflineAttendanceService.Result send(Map<String,Object> scan) {return service.sync(List.of(scan),actor).getFirst();}
    private int count(String table) {return jdbc.queryForObject("SELECT count(*) FROM "+table,Integer.class);}

    @Test void snapshotIsScopedToPublishedPairsAndContainsVisualLookupFields() {
        assertEquals(1,snapshot.assignments().size()); assertEquals(1,snapshot.students().size());
        var student=snapshot.students().getFirst(); assertEquals("2022000001",student.get("computerNumber"));
        assertEquals("/photo1",student.get("photoPath")); assertFalse(student.containsKey("qrToken"));
    }
    @Test void acceptedRetryIsIdenticalAndRetainsCapturedTime() {
        var scan=scan(); var first=send(scan); assertEquals("ACCEPTED",first.outcome());
        scan.put("computerNumber","2022000002"); // A UUID denotes the first outcome, even with changed payload.
        assertEquals(first,send(scan)); assertEquals(1,count("attendance")); assertEquals(1,count("attendance_sync_scan"));
        var row=jdbc.queryForMap("SELECT * FROM attendance");
        LocalDateTime captured=LocalDateTime.ofInstant(Instant.parse((String)scan.get("capturedAt")),ZoneId.systemDefault());
        assertTrue(Math.abs(Duration.between(captured,((java.sql.Timestamp)row.get("check_in_time")).toLocalDateTime()).toNanos())<1000);
        assertNotNull(row.get("processed_at")); assertEquals(UUID.fromString((String)scan.get("scanId")),row.get("client_scan_id"));
    }
    @Test void malformedAndUnlistedItemsDoNotBlockValidItemsAndRejectionsPersist() {
        var missing=scan(); missing.put("computerNumber","2022000002");
        var malformed=scan(); malformed.put("examSessionId","bad");
        var results=service.sync(List.of("bad item",missing,malformed,scan()),actor);
        assertEquals(List.of("REJECTED","REJECTED","REJECTED","ACCEPTED"),results.stream().map(OfflineAttendanceService.Result::outcome).toList());
        assertEquals("NOT_IN_SNAPSHOT",results.get(1).reason()); assertEquals(results.get(1),send(missing));
        assertEquals(3,count("attendance_sync_scan")); assertEquals(1,count("attendance"));
    }
    @Test void anotherDeviceDoesNotOverwriteAttendance() {
        var first=send(scan()); var second=send(scan());
        assertEquals("ALREADY_RECORDED",second.outcome()); assertEquals(first.attendanceId(),second.attendanceId());
        assertEquals(1,count("attendance")); assertEquals(2,count("attendance_sync_scan"));
    }
    @Test void changedAllocationAndCompletedExamAreRejected() {
        jdbc.update("UPDATE student_venue_allocation SET venue_id=2 WHERE exam_session_id=1");
        var rejected=scan(); assertEquals("ALLOCATION_CHANGED",send(rejected).reason());
        jdbc.update("UPDATE student_venue_allocation SET venue_id=1 WHERE exam_session_id=1");
        assertEquals("ALLOCATION_CHANGED",send(rejected).reason());
        jdbc.update("UPDATE exam_session SET status='COMPLETED' WHERE exam_session_id=1");
        assertEquals("EXAM_COMPLETED",send(scan()).reason()); assertEquals(0,count("attendance"));
    }
    @Test void withdrawnPublicationAssignmentAndEligibilityAreRejected() {
        jdbc.update("UPDATE exam_session SET schedule_published=false WHERE exam_session_id=1");
        assertEquals("EXAM_NOT_PUBLISHED",send(scan()).reason());
        jdbc.update("UPDATE exam_session SET schedule_published=true WHERE exam_session_id=1");
        jdbc.update("DELETE FROM student_registration"); assertEquals("NOT_ELIGIBLE",send(scan()).reason());
        jdbc.update("DELETE FROM invigilator_assignment WHERE staff_id=1"); assertEquals("NOT_ASSIGNED",send(scan()).reason());
        assertEquals(0,count("attendance"));
    }
    @Test void qrCannotBeOmittedOrBypassedWithComputerVerification() {
        var scan=scan();scan.put("verificationMethod","QR_AND_FACE");assertEquals("INVALID_QR",send(scan).reason());
        scan=scan();scan.put("qrToken","invalid");when(qr.parseAndValidate("invalid")).thenThrow(new IllegalArgumentException("Invalid examination pass QR token"));
        assertEquals("INVALID_QR",send(scan).reason());assertEquals(0,count("attendance"));
    }
    @Test void anotherAccountCannotUseSnapshotOrReadOutcome() {
        var scan=scan();send(scan);var other=new Staff();other.setStaffId(2);
        assertEquals("SCAN_ID_OWNED_BY_ANOTHER_USER",service.sync(List.of(scan),other).getFirst().reason());
        assertEquals("INVALID_SNAPSHOT",service.sync(List.of(scan()),other).getFirst().reason());
    }
    @Test void absentAttendanceIsNeverConvertedToPresent() {
        jdbc.update("INSERT INTO attendance(computer_number,exam_session_id,attendance_status) VALUES ('2022000001',1,'ABSENT')");
        assertEquals("ATTENDANCE_CONFLICT",send(scan()).reason());
        assertEquals("ABSENT",jdbc.queryForObject("SELECT attendance_status FROM attendance",String.class));
    }
    @Test void invalidCaptureTimeIsRejected() {
        var scan=scan();scan.put("capturedAt",Instant.now().plusSeconds(600).toString());assertEquals("INVALID_CAPTURE_TIME",send(scan).reason());
        scan=scan();scan.put("capturedAt",Instant.now().minusSeconds(600).toString());assertEquals("INVALID_CAPTURE_TIME",send(scan).reason());
        scan=scan();scan.put("capturedAt","2026-10-01T10:00:00");assertEquals("INVALID_CAPTURE_TIME",send(scan).reason());
    }
    @Test void simultaneousRetriesAndDifferentDevicesRemainIdempotent() throws Exception {
        var scan=scan();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->send(scan));var second=pool.submit(()->send(scan));
            assertEquals(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));
        }
        assertEquals(1,count("attendance"));assertEquals(1,count("attendance_sync_scan"));
        jdbc.execute("TRUNCATE attendance, attendance_sync_scan");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->send(scan()));var second=pool.submit(()->send(scan()));
            assertEquals(Set.of("ACCEPTED","ALREADY_RECORDED"),Set.of(first.get(10,TimeUnit.SECONDS).outcome(),second.get(10,TimeUnit.SECONDS).outcome()));
        }
        assertEquals(1,count("attendance"));
    }
}

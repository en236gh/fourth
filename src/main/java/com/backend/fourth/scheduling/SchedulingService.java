package com.backend.fourth.scheduling;

import com.backend.fourth.invigilator.repository.AssignmentWriteLock;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import static com.backend.fourth.scheduling.ConstraintScheduler.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SchedulingService {
    private final JdbcTemplate jdbc;
    private final AssignmentWriteLock writeLock;
    private final SchedulingAccess access;
    @Value("${app.institution-timezone:Africa/Lusaka}")
    private String timezone = "Africa/Lusaka";

    public record Validation(boolean valid, List<String> problems) {}
    public record Generation(Result result, Map<String,Object> period) {}

    public List<Map<String,Object>> list() {
        return jdbc.queryForList("""
            SELECT period_id,name,academic_year,semester,exam_type,start_date,end_date,timezone,status,revision,published_at
            FROM examination_period ORDER BY start_date DESC, period_id DESC
            """);
    }

    public Map<String,Object> defaults() {
        return Map.of("academicYear", currentAcademicYear());
    }

    private String currentAcademicYear() {
        String year = jdbc.queryForObject("SELECT max(academic_year) FROM student_registration", String.class);
        if (year == null || !year.matches("[0-9]{4}/[0-9]{4}"))
            throw new IllegalStateException("No valid academic year is available in student registrations. Load registrations before creating an examination period.");
        return year;
    }

    @Transactional
    public Map<String,Object> create(SchedulingRequests.Period request) {
        writeLock.acquire();
        access.administrator();
        List<SchedulingRequests.DailySlot> slots=validateSetup(request);
        String academicYear=currentAcademicYear();
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE academic_year=? AND semester=? AND exam_type=?)",
                Boolean.class, academicYear,request.semester(),request.examType())))
            throw new IllegalStateException("An examination period already exists for this academic year, semester and exam type.");
        Integer id = jdbc.queryForObject("""
                INSERT INTO examination_period(name,academic_year,semester,exam_type,start_date,end_date,timezone)
                VALUES (?,?,?,?,?,?,?) RETURNING period_id
                """, Integer.class,request.name().trim(),academicYear,request.semester(),request.examType(),request.startDate(),request.endDate(),timezone);
        for (int day : request.daysOfWeek()) for (var slot : slots)
            jdbc.update("INSERT INTO examination_period_slot VALUES (?,?,?,?)",id,day,slot.startTime(),slot.endTime());
        return detail(id);
    }

    private List<SchedulingRequests.DailySlot> validateSetup(SchedulingRequests.Period request) {
        ZoneId.of(timezone);
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE timezone<>?)",Boolean.class,timezone)))
            throw new IllegalStateException("Institution timezone differs from existing periods. Restore the configured timezone before scheduling.");
        if (request.endDate().isBefore(request.startDate()) || ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > 366)
            throw new IllegalArgumentException("Period must be between 1 and 367 days.");
        if (new HashSet<>(request.daysOfWeek()).size()!=request.daysOfWeek().size())
            throw new IllegalArgumentException("Duplicate weekdays.");
        List<SchedulingRequests.DailySlot> slots = request.slots().stream().sorted(Comparator.comparing(SchedulingRequests.DailySlot::startTime)).toList();
        for (int i=0;i<slots.size();i++) {
            if (!slots.get(i).startTime().isBefore(slots.get(i).endTime())) throw new IllegalArgumentException("Daily slot end must follow start.");
            if (i>0 && slots.get(i).startTime().isBefore(slots.get(i-1).endTime())) throw new IllegalArgumentException("Daily slots must not overlap.");
        }
        if (request.startDate().datesUntil(request.endDate().plusDays(1)).noneMatch(d -> request.daysOfWeek().contains(d.getDayOfWeek().getValue())))
            throw new IllegalArgumentException("No allowed weekdays occur in this period.");
        return slots;
    }

    @Transactional
    public Map<String,Object> update(int id,long revision,SchedulingRequests.Period request) {
        Map<String,Object> existing=editable(id,revision);
        String academicYear=(String)existing.get("academic_year");
        if (!sessions(id).isEmpty()) throw new IllegalStateException("Reset the draft before changing the examination window.");
        var slots=validateSetup(request);
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id<>? AND academic_year=? AND semester=? AND exam_type=?)",
                Boolean.class,id,academicYear,request.semester(),request.examType()))) throw new IllegalStateException("This examination cycle already has a period.");
        jdbc.update("UPDATE examination_period SET name=?,academic_year=?,semester=?,exam_type=?,start_date=?,end_date=?,revision=revision+1 WHERE period_id=?",
                request.name().trim(),academicYear,request.semester(),request.examType(),request.startDate(),request.endDate(),id);
        jdbc.update("DELETE FROM examination_period_slot WHERE period_id=?",id);
        for (int day:request.daysOfWeek()) for(var slot:slots) jdbc.update("INSERT INTO examination_period_slot VALUES (?,?,?,?)",id,day,slot.startTime(),slot.endTime());
        return detail(id);
    }

    public Map<String,Object> detail(int id) {
        Map<String,Object> result = new LinkedHashMap<>(period(id));
        result.remove("coordinator_staff_id");
        result.put("canEdit",access.canEdit(id));
        result.put("slots",jdbc.queryForList("SELECT day_of_week,start_time,end_time FROM examination_period_slot WHERE period_id=? ORDER BY day_of_week,start_time",id));
        result.put("courses",jdbc.queryForList("""
                SELECT pc.course_code,c.course_name,pc.duration_minutes,
                (SELECT count(*) FROM student_registration r WHERE r.course_code=pc.course_code AND r.academic_year=p.academic_year AND r.semester=p.semester) AS eligible_students
                FROM examination_period_course pc JOIN examination_period p USING(period_id) JOIN course c USING(course_code)
                WHERE pc.period_id=? ORDER BY pc.course_code
                """,id));
        result.put("exams",jdbc.queryForList("""
                SELECT e.*, (SELECT count(*) FROM student_registration r WHERE r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester) registered_students,
                (SELECT count(*) FROM student_venue_allocation a WHERE a.exam_session_id=e.exam_session_id) allocated_students
                FROM exam_session e WHERE period_id=? ORDER BY exam_date,start_time,course_code
                """,id));
        result.put("bookings",jdbc.queryForList("""
                SELECT ev.*,v.venue_name,v.examination_capacity,
                (SELECT count(*) FROM student_venue_allocation a WHERE a.exam_session_id=ev.exam_session_id AND a.venue_id=ev.venue_id) allocated_students
                FROM exam_venue ev JOIN exam_session e USING(exam_session_id) JOIN venue v USING(venue_id)
                WHERE e.period_id=? ORDER BY ev.exam_session_id,ev.venue_id
                """,id));
        result.put("allocations",jdbc.queryForList("""
                SELECT a.*,s.full_name FROM student_venue_allocation a JOIN exam_session e USING(exam_session_id)
                JOIN student s USING(computer_number) WHERE e.period_id=? ORDER BY a.exam_session_id,a.computer_number
                """,id));
        result.put("unallocatedStudents",jdbc.queryForList("""
                SELECT pc.course_code,r.computer_number FROM examination_period_course pc JOIN examination_period p USING(period_id)
                JOIN student_registration r ON r.course_code=pc.course_code AND r.academic_year=p.academic_year AND r.semester=p.semester
                WHERE p.period_id=? AND NOT EXISTS(SELECT 1 FROM exam_session e JOIN student_venue_allocation a USING(exam_session_id)
                    WHERE e.period_id=p.period_id AND e.course_code=pc.course_code AND a.computer_number=r.computer_number)
                ORDER BY pc.course_code,r.computer_number
                """,id));
        result.put("conflictingExams",jdbc.queryForList("""
                SELECT e.*, (SELECT count(*) FROM attendance a WHERE a.exam_session_id=e.exam_session_id) attendance_records,
                (SELECT count(*) FROM incident a WHERE a.exam_session_id=e.exam_session_id) incident_records,
                (SELECT count(*) FROM generated_report a WHERE a.exam_session_id=e.exam_session_id) report_records,
                (SELECT count(*) FROM invigilator_assignment a WHERE a.exam_session_id=e.exam_session_id) staffing_records,
                (SELECT count(*) FROM student_venue_allocation a WHERE a.exam_session_id=e.exam_session_id) allocation_records,
                (SELECT count(*) FROM exam_venue a WHERE a.exam_session_id=e.exam_session_id) booking_records
                FROM exam_session e JOIN examination_period_course pc ON pc.course_code=e.course_code AND pc.period_id=?
                JOIN examination_period p ON p.period_id=pc.period_id
                WHERE e.period_id IS DISTINCT FROM p.period_id AND e.academic_year=p.academic_year AND e.semester=p.semester AND e.exam_type=p.exam_type
                ORDER BY e.exam_session_id
                """,id));
        result.put("assignments",jdbc.queryForList("SELECT a.* FROM invigilator_assignment a JOIN exam_session e USING(exam_session_id) WHERE e.period_id=? ORDER BY exam_session_id,venue_id,staff_id",id));
        return result;
    }

    public List<Map<String,Object>> catalog(int id, Integer schoolId) {
        var p=period(id);
        return jdbc.queryForList("""
                SELECT c.course_code,c.course_name,
                (SELECT count(*) FROM student_registration r WHERE r.course_code=c.course_code AND r.academic_year=? AND r.semester=?) eligible_students
                FROM course c WHERE c.is_active AND (CAST(? AS integer) IS NULL OR EXISTS (
                    SELECT 1 FROM programme_course pc JOIN programme pr USING(programme_id)
                    WHERE pc.course_code=c.course_code AND pc.is_active AND pr.is_active AND pr.school_id=?)) ORDER BY c.course_code
                """,p.get("academic_year"),p.get("semester"),schoolId,schoolId);
    }

    @Transactional
    public Map<String,Object> select(int id, SchedulingRequests.Selection request) {
        editable(id,request.revision());
        if (!sessions(id).isEmpty()) throw new IllegalStateException("Reset the draft before changing selected courses; review the existing allocations first.");
        if (request.courses().stream().map(SchedulingRequests.Course::courseCode).distinct().count()!=request.courses().size())
            throw new IllegalArgumentException("Duplicate course selection.");
        for (var course : request.courses()) {
            if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM course WHERE course_code=? AND is_active)",Boolean.class,course.courseCode())))
                throw new IllegalArgumentException("Unknown or inactive course: "+course.courseCode());
        }
        jdbc.update("DELETE FROM examination_period_course WHERE period_id=?",id);
        for (var course : request.courses()) jdbc.update("INSERT INTO examination_period_course VALUES (?,?,?)",id,course.courseCode(),course.durationMinutes());
        bump(id); return detail(id);
    }

    @Transactional
    public Map<String,Object> reset(int id,long revision) {
        editable(id,revision); requireReplaceable(id);
        // This explicit operation discards only unpublished generated drafts, never legacy data.
        deleteDraft(id); bump(id); return detail(id);
    }

    @Transactional
    public Generation generate(int id, SchedulingRequests.Generate request) {
        Map<String,Object> p=editable(id,request.revision()); requireReplaceable(id);
        List<Exam> exams=exams(p);
        List<String> duplicates=jdbc.queryForList("""
                SELECT DISTINCT e.course_code FROM exam_session e JOIN examination_period_course pc ON pc.course_code=e.course_code AND pc.period_id=?
                WHERE e.period_id IS DISTINCT FROM ? AND e.academic_year=? AND e.semester=? AND e.exam_type=? ORDER BY e.course_code
                """,String.class,id,id,p.get("academic_year"),p.get("semester"),p.get("exam_type"));
        if (!duplicates.isEmpty()) return new Generation(new Result(Outcome.INVALID_INPUT,List.of(),duplicates,0,
                List.of("Existing sessions already belong to this course/cycle. Review legacy sessions before generating; ownership is never guessed.")),detail(id));
        List<Room> rooms=rooms();
        ConstraintScheduler scheduler=new ConstraintScheduler(slots(p),rooms,reservations(id),request.searchLimit());
        Result result=scheduler.solve(exams);
        if (result.outcome()!=Outcome.COMPLETE) return new Generation(result,detail(id));
        deleteDraft(id);
        for (Placement placement : result.placements()) {
            Integer session=jdbc.queryForObject("""
                    INSERT INTO exam_session(course_code,exam_date,start_time,end_time,academic_year,semester,exam_type,status,period_id,schedule_published)
                    VALUES (?,?,?,?,?,?,?,'SCHEDULED',?,false) RETURNING exam_session_id
                    """,Integer.class,placement.course(),placement.start().toLocalDate(),placement.start().toLocalTime(),placement.end().toLocalTime(),
                    p.get("academic_year"),p.get("semester"),p.get("exam_type"),id);
            jdbc.update("""
                    INSERT INTO exam_session_programme_course(exam_session_id,programme_course_id)
                    SELECT ?,programme_course_id FROM programme_course WHERE course_code=? AND semester=? AND is_active ON CONFLICT DO NOTHING
                    """,session,placement.course(),p.get("semester"));
            saveAllocations(session,placement,rooms);
        }
        List<String> problems=validateInternal(id,false);
        if (!problems.isEmpty()) throw new IllegalStateException("Draft validation failed: "+String.join("; ",problems));
        bump(id); return new Generation(result,detail(id));
    }

    @Transactional
    public Map<String,Object> edit(int id,int session,SchedulingRequests.Edit request) {
        Map<String,Object> p=editable(id,request.revision());
        Map<String,Object> row=sessions(id).stream().filter(e -> number(e,"exam_session_id")==session).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exam does not belong to this period."));
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM invigilator_assignment WHERE exam_session_id=? AND assignment_status<>'CANCELLED')",Boolean.class,session)))
            throw new IllegalStateException("Cancel the exam's invigilator assignments before moving it; then assign available staff again.");
        if (!"SCHEDULED".equals(row.get("status")) || !((Date)row.get("exam_date")).toLocalDate().atTime(((Time)row.get("start_time")).toLocalTime()).isAfter(LocalDateTime.now(ZoneId.of(timezone))))
            throw new IllegalStateException("Started or completed examinations cannot be moved through draft editing.");
        Exam exam=exams(p).stream().filter(e -> e.course().equals(row.get("course_code"))).findFirst().orElseThrow();
        LocalDateTime start=request.examDate().atTime(request.startTime());
        Placement replacement=new Placement(exam.course(),start,start.plusMinutes(exam.minutes()),request.venueIds(),exam.students());
        List<Placement> candidate=new ArrayList<>(placements(id));
        candidate.removeIf(e -> e.course().equals(exam.course())); candidate.add(replacement);
        List<String> problems=new ConstraintScheduler(slots(p),rooms(),reservations(id),1).validate(exams(p),candidate);
        if (!problems.isEmpty()) throw new IllegalArgumentException(String.join("; ",problems));
        jdbc.update("DELETE FROM student_venue_allocation WHERE exam_session_id=?",session);
        jdbc.update("DELETE FROM exam_venue WHERE exam_session_id=?",session);
        jdbc.update("UPDATE exam_session SET exam_date=?,start_time=?,end_time=? WHERE exam_session_id=?",request.examDate(),request.startTime(),replacement.end().toLocalTime(),session);
        saveAllocations(session,replacement,rooms());
        problems=validateInternal(id,false);
        if (!problems.isEmpty()) throw new IllegalStateException(String.join("; ",problems));
        bump(id); return detail(id);
    }

    public Validation validate(int id) {
        List<String> problems=validateInternal(id,true);
        return new Validation(problems.isEmpty(),problems);
    }

    @Transactional
    public Map<String,Object> publish(int id,long revision) {
        editable(id,revision);
        List<String> problems=validateInternal(id,true);
        if (!problems.isEmpty()) throw new IllegalStateException("Publication blocked: "+String.join("; ",problems));
        jdbc.update("UPDATE invigilator_assignment SET assignment_status='PUBLISHED',published_at=? WHERE assignment_status='DRAFT' AND exam_session_id IN(SELECT exam_session_id FROM exam_session WHERE period_id=?)",LocalDateTime.now(ZoneId.of(timezone)),id);
        jdbc.update("UPDATE exam_session SET schedule_published=true WHERE period_id=?",id);
        jdbc.update("UPDATE examination_period SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP,revision=revision+1 WHERE period_id=?",id);
        return detail(id);
    }

    public List<Map<String,Object>> venues() {
        return jdbc.queryForList("SELECT venue_id,venue_name,building,capacity,examination_capacity FROM venue ORDER BY venue_id");
    }

    @Transactional
    public void capacity(int venue,int capacity) {
        writeLock.acquire();
        access.administrator();
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM student_venue_allocation WHERE venue_id=? GROUP BY exam_session_id HAVING count(*)>?)",Boolean.class,venue,capacity)))
            throw new IllegalStateException("Capacity is below an existing exam's allocation count.");
        if (jdbc.update("UPDATE venue SET examination_capacity=? WHERE venue_id=?",capacity,venue)!=1) throw new IllegalArgumentException("Venue not found.");
    }

    public List<Map<String,Object>> unavailability() {
        return jdbc.queryForList("SELECT * FROM venue_unavailability ORDER BY starts_at,venue_id");
    }

    @Transactional
    public void unavailable(int venue,SchedulingRequests.Unavailability request) {
        writeLock.acquire();
        access.administrator();
        if (!request.startsAt().isBefore(request.endsAt())) throw new IllegalArgumentException("Unavailability end must follow start.");
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM exam_venue v JOIN exam_session e USING(exam_session_id)
                WHERE v.venue_id=? AND e.exam_date+e.start_time<? AND e.exam_date+e.end_time>?)
                """,Boolean.class,venue,request.endsAt(),request.startsAt()))) throw new IllegalStateException("Unavailability overlaps an existing exam booking.");
        jdbc.update("INSERT INTO venue_unavailability(venue_id,starts_at,ends_at,reason) VALUES (?,?,?,?)",venue,request.startsAt(),request.endsAt(),request.reason());
    }

    public List<Map<String,Object>> audit() { return jdbc.queryForList("SELECT * FROM exam_allocation_audit ORDER BY exam_session_id,computer_number"); }

    public List<Map<String,Object>> history(int id) {
        period(id);
        return jdbc.queryForList("SELECT * FROM scheduling_audit WHERE period_id=? ORDER BY audit_id",id);
    }

    /** Bounded suggestions keep every other examination fixed. Save revalidates current data. */
    public List<Placement> alternatives(int id, int session) {
        var p=period(id);
        var row=sessions(id).stream().filter(e -> number(e,"exam_session_id")==session).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exam does not belong to this period."));
        var exam=exams(p).stream().filter(e -> e.course().equals(row.get("course_code"))).findFirst().orElseThrow();
        List<Placement> occupied=new ArrayList<>(reservations(id));
        occupied.addAll(placements(id).stream().filter(e -> !e.course().equals(exam.course())).toList());
        List<Placement> choices=new ArrayList<>();
        for (var slot:slots(p)) {
            var result=new ConstraintScheduler(List.of(slot),rooms(),occupied,1000).solve(List.of(exam));
            if (result.outcome()==Outcome.COMPLETE) choices.addAll(result.placements());
            if (choices.size()==10) break;
        }
        return choices;
    }

    public void validateAmendmentPlacement(int id,int session,LocalDate date,LocalTime start,LocalTime end,List<Integer> venues) {
        var p=period(id);
        if(!timezone.equals(p.get("timezone"))) throw new IllegalStateException("Institution timezone differs from this period.");
        if(!date.atTime(start).isAfter(LocalDateTime.now(ZoneId.of(timezone)))) throw new IllegalArgumentException("Proposed start must be in the future.");
        var row=sessions(id).stream().filter(e->number(e,"exam_session_id")==session).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Exam does not belong to this period."));
        var current=placements(id);
        var original=current.stream().filter(e->e.course().equals(row.get("course_code"))).findFirst().orElseThrow();
        List<Placement> proposed=new ArrayList<>(current);
        proposed.remove(original);proposed.add(new Placement(original.course(),date.atTime(start),date.atTime(end),venues,original.students()));
        var problems=new ConstraintScheduler(slots(p),rooms(),reservations(id),1).validate(exams(p),proposed);
        if(!problems.isEmpty()) throw new IllegalStateException("Amendment conflicts: "+String.join("; ",problems));
    }

    private Map<String,Object> editable(int id,long revision) {
        writeLock.acquire();
        access.administrator();
        Map<String,Object> p=period(id);
        if (!timezone.equals(p.get("timezone"))) throw new IllegalStateException("Institution timezone differs from this period.");
        if (!"DRAFT".equals(p.get("status"))) throw new IllegalStateException("Published periods are locked. No timetable or allocation changes were made.");
        if (((Number)p.get("revision")).longValue()!=revision) throw new IllegalStateException("Draft changed since it was loaded. Refresh before retrying.");
        return p;
    }
    private Map<String,Object> period(int id) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM examination_period WHERE period_id=?",id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Examination period not found.");
        return rows.getFirst();
    }
    private void bump(int id) { jdbc.update("UPDATE examination_period SET revision=revision+1 WHERE period_id=?",id); }
    private List<Map<String,Object>> sessions(int id) { return jdbc.queryForList("SELECT * FROM exam_session WHERE period_id=? ORDER BY course_code",id); }
    private void requireReplaceable(int id) {
        if (Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM exam_session e WHERE e.period_id=? AND (e.status<>'SCHEDULED' OR e.schedule_published
                OR e.exam_date+e.start_time<=CURRENT_TIMESTAMP AT TIME ZONE ?
                OR EXISTS(SELECT 1 FROM invigilator_assignment a WHERE a.exam_session_id=e.exam_session_id)
                OR EXISTS(SELECT 1 FROM attendance a WHERE a.exam_session_id=e.exam_session_id)
                OR EXISTS(SELECT 1 FROM incident a WHERE a.exam_session_id=e.exam_session_id)
                OR EXISTS(SELECT 1 FROM generated_report a WHERE a.exam_session_id=e.exam_session_id)))
                """,Boolean.class,id,timezone))) throw new IllegalStateException("Draft has staffing or operational records. Cancel assignments before regenerating; operational records are never discarded.");
    }
    private void deleteDraft(int id) {
        jdbc.update("DELETE FROM student_venue_allocation WHERE exam_session_id IN(SELECT exam_session_id FROM exam_session WHERE period_id=?)",id);
        jdbc.update("DELETE FROM exam_session_programme_course WHERE exam_session_id IN(SELECT exam_session_id FROM exam_session WHERE period_id=?)",id);
        jdbc.update("DELETE FROM exam_venue WHERE exam_session_id IN(SELECT exam_session_id FROM exam_session WHERE period_id=?)",id);
        jdbc.update("DELETE FROM exam_session WHERE period_id=?",id);
    }
    private List<Room> rooms() {
        return jdbc.query("SELECT venue_id,examination_capacity FROM venue WHERE examination_capacity>0 ORDER BY venue_id",(r,n)->new Room(r.getInt(1),r.getInt(2)));
    }
    private List<Exam> exams(Map<String,Object> p) {
        return jdbc.queryForList("SELECT * FROM examination_period_course WHERE period_id=? ORDER BY course_code",p.get("period_id")).stream()
                .map(c -> new Exam((String)c.get("course_code"),number(c,"duration_minutes"),students((String)c.get("course_code"),p.get("academic_year"),p.get("semester")))).toList();
    }
    private Set<String> students(String course,Object year,Object semester) {
        return new TreeSet<>(jdbc.queryForList("SELECT computer_number FROM student_registration WHERE course_code=? AND academic_year=? AND semester=? ORDER BY computer_number",String.class,course,year,semester));
    }
    private List<Slot> slots(Map<String,Object> p) {
        List<Slot> result=new ArrayList<>();
        var templates=jdbc.queryForList("SELECT * FROM examination_period_slot WHERE period_id=? ORDER BY day_of_week,start_time",p.get("period_id"));
        LocalDate start=((Date)p.get("start_date")).toLocalDate(),end=((Date)p.get("end_date")).toLocalDate();
        for (LocalDate day=start; !day.isAfter(end); day=day.plusDays(1)) for (var t:templates)
            if (number(t,"day_of_week")==day.getDayOfWeek().getValue()) result.add(new Slot(day.atTime(((Time)t.get("start_time")).toLocalTime()),day.atTime(((Time)t.get("end_time")).toLocalTime())));
        return result;
    }
    private List<Placement> reservations(int id) {
        List<Placement> result=new ArrayList<>();
        for(var e:jdbc.queryForList("SELECT * FROM exam_session WHERE period_id IS DISTINCT FROM ? ORDER BY exam_session_id",id)) {
            int session=number(e,"exam_session_id");
            Set<String> enrolled=students((String)e.get("course_code"),e.get("academic_year"),e.get("semester"));
            enrolled.addAll(jdbc.queryForList("SELECT computer_number FROM student_venue_allocation WHERE exam_session_id=?",String.class,session));
            result.add(placement(e,enrolled));
        }
        for(var b:unavailability()) result.add(new Placement("Unavailable venue "+b.get("venue_id"),((Timestamp)b.get("starts_at")).toLocalDateTime(),
                ((Timestamp)b.get("ends_at")).toLocalDateTime(),List.of(number(b,"venue_id")),Set.of()));
        return result;
    }
    private List<Placement> placements(int id) {
        return sessions(id).stream().map(e -> placement(e,new TreeSet<>(jdbc.queryForList("SELECT computer_number FROM student_venue_allocation WHERE exam_session_id=?",String.class,e.get("exam_session_id"))))).toList();
    }
    private Placement placement(Map<String,Object> e,Set<String> students) {
        LocalDate day=((Date)e.get("exam_date")).toLocalDate();
        return new Placement((String)e.get("course_code"),day.atTime(((Time)e.get("start_time")).toLocalTime()),day.atTime(((Time)e.get("end_time")).toLocalTime()),
                jdbc.queryForList("SELECT venue_id FROM exam_venue WHERE exam_session_id=? ORDER BY venue_id",Integer.class,e.get("exam_session_id")),students);
    }
    private void saveAllocations(int session,Placement p,List<Room> rooms) {
        Map<Integer,Integer> capacities=rooms.stream().collect(Collectors.toMap(Room::id,Room::capacity));
        Iterator<String> students=new TreeSet<>(p.students()).iterator();
        for (int venue : p.venues().stream().sorted().toList()) {
            jdbc.update("INSERT INTO exam_venue VALUES (?,?)",session,venue);
            for (int n=0; n<capacities.get(venue) && students.hasNext(); n++)
                jdbc.update("INSERT INTO student_venue_allocation(computer_number,exam_session_id,venue_id) VALUES (?,?,?)",students.next(),session,venue);
        }
        if (students.hasNext()) throw new IllegalStateException("Insufficient examination capacity.");
    }
    private List<String> validateInternal(int id,boolean staffing) {
        Map<String,Object> p=period(id);
        List<String> problems=new ArrayList<>();
        List<Exam> exams=exams(p);
        if (exams.isEmpty()) problems.add("Select at least one course.");
        for (Exam e:exams) if (e.students().isEmpty()) problems.add(e.course()+": no eligible registrations.");
        problems.addAll(new ConstraintScheduler(slots(p),rooms(),reservations(id),1).validate(exams,placements(id)));
        for (var e:sessions(id)) {
            int session=number(e,"exam_session_id");
            if (!Objects.equals(e.get("academic_year"),p.get("academic_year")) || number(e,"semester")!=number(p,"semester") || !Objects.equals(e.get("exam_type"),p.get("exam_type"))) problems.add("Exam "+session+": cycle does not match period.");
            for (var row:jdbc.queryForList("""
                    SELECT a.venue_id,count(*) n FROM student_venue_allocation a LEFT JOIN exam_venue ev ON ev.exam_session_id=a.exam_session_id AND ev.venue_id=a.venue_id
                    LEFT JOIN venue v ON v.venue_id=a.venue_id WHERE a.exam_session_id=? GROUP BY a.venue_id,ev.venue_id,v.examination_capacity
                    HAVING ev.venue_id IS NULL OR v.examination_capacity IS NULL OR count(*)>v.examination_capacity
                    """,session)) problems.add("Exam "+session+", venue "+row.get("venue_id")+": missing booking or over capacity.");
            if (staffing) {
                for (var invalid:jdbc.queryForList("""
                        SELECT a.staff_id FROM invigilator_assignment a JOIN staff s USING(staff_id)
                        WHERE a.exam_session_id=? AND a.assignment_status<>'CANCELLED' AND (s.account_status<>'ACTIVE'
                        OR NOT EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=a.staff_id AND r.name='INVIGILATOR'))
                        """,session)) problems.add("Exam "+session+": assigned staff "+invalid.get("staff_id")+" is not an active invigilator.");
                if (!"PUBLISHED".equals(p.get("status")) && !((Date)e.get("exam_date")).toLocalDate().atTime(((Time)e.get("start_time")).toLocalTime()).isAfter(LocalDateTime.now(ZoneId.of((String)p.get("timezone"))))) problems.add("Exam "+session+": start must be in the future for publication.");
                for (int venue:jdbc.queryForList("SELECT venue_id FROM exam_venue WHERE exam_session_id=?",Integer.class,session)) {
                    long allocated=jdbc.queryForObject("SELECT count(*) FROM student_venue_allocation WHERE exam_session_id=? AND venue_id=?",Long.class,session,venue);
                    long assigned=jdbc.queryForObject("""
                            SELECT count(*) FROM invigilator_assignment a JOIN staff s USING(staff_id)
                            WHERE a.exam_session_id=? AND a.venue_id=? AND a.assignment_status IN ('DRAFT','PUBLISHED') AND s.account_status='ACTIVE'
                            AND EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=a.staff_id AND r.name='INVIGILATOR')
                            """,Long.class,session,venue);
                    if (assigned<Math.max(1,(allocated+49)/50)) problems.add("Exam "+session+", venue "+venue+": insufficient active invigilators (one per 50 students, minimum one).");
                }
            }
        }
        // Invigilator clashes must also be checked after manual placement changes.
        for(var conflict:jdbc.queryForList("""
                SELECT DISTINCT a.staff_id,a.exam_session_id FROM invigilator_assignment a JOIN exam_session e USING(exam_session_id)
                JOIN invigilator_assignment b ON b.staff_id=a.staff_id AND (b.exam_session_id,b.venue_id)<>(a.exam_session_id,a.venue_id)
                JOIN exam_session other ON other.exam_session_id=b.exam_session_id
                WHERE e.period_id=? AND a.assignment_status<>'CANCELLED' AND b.assignment_status<>'CANCELLED'
                AND e.exam_date+e.start_time<other.exam_date+other.end_time AND e.exam_date+e.end_time>other.exam_date+other.start_time
                """,id)) problems.add("Invigilator "+conflict.get("staff_id")+": overlapping duties for exam "+conflict.get("exam_session_id"));
        return problems.stream().distinct().toList();
    }
    private static int number(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
}

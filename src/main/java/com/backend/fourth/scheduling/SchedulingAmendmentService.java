package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.repository.AssignmentWriteLock;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SchedulingAmendmentService {
    private final JdbcTemplate jdbc;
    private final SchedulingAccess access;
    private final CurrentStaffResolver staff;
    private final AssignmentWriteLock lock;
    private final SchedulingService scheduling;
    private final SchedulingNotificationService notifications;
    private final org.springframework.context.ApplicationEventPublisher events;
    public record Duty(@Positive int venueId,@Positive int staffId) {}
    public record Proposal(@Positive int examSessionId,@Min(0) long revision,@NotNull LocalDate examDate,
                           @NotNull LocalTime startTime,@NotBlank @Size(max=2000) String reason,
                           @Size(min=1,max=100) List<@NotNull @Positive Integer> venueIds,
                           @Size(min=1,max=1000) List<@NotNull @Valid Duty> duties) {
        public Proposal(int examSessionId,long revision,LocalDate date,LocalTime start,String reason) {
            this(examSessionId,revision,date,start,reason,null,null);
        }
    }
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String status,
                           @NotBlank @Size(max=2000) String reason) {}

    public List<Map<String,Object>> list(int period) {
        return jdbc.queryForList("SELECT * FROM examination_amendment WHERE period_id=? ORDER BY amendment_id DESC",period).stream()
                .map(this::withScope).toList();
    }
    private Map<String,Object> amendment(int period,long id) {
        var rows=jdbc.queryForList("SELECT * FROM examination_amendment WHERE period_id=? AND amendment_id=?",period,id);
        if(rows.isEmpty()) throw new IllegalArgumentException("Amendment not found.");return withScope(rows.getFirst());
    }
    private Map<String,Object> requireFutureExam(int period,int exam,long revision) {
        var rows=jdbc.queryForList("""
                SELECT e.*,p.revision,p.timezone,p.status period_status FROM exam_session e JOIN examination_period p USING(period_id)
                WHERE e.period_id=? AND e.exam_session_id=?
                """,period,exam);
        if(rows.isEmpty()) throw new IllegalArgumentException("Exam does not belong to this period.");
        var row=rows.getFirst();
        if(!"PUBLISHED".equals(row.get("period_status")) || !Boolean.TRUE.equals(row.get("schedule_published"))) throw new IllegalStateException("Amendments require a published timetable.");
        if(((Number)row.get("revision")).longValue()!=revision) throw new IllegalStateException("Timetable changed. Submit a new proposal against the current revision.");
        if(!"SCHEDULED".equals(row.get("status")) || !((Date)row.get("exam_date")).toLocalDate().atTime(((Time)row.get("start_time")).toLocalTime()).isAfter(LocalDateTime.now(ZoneId.of((String)row.get("timezone")))))
            throw new IllegalStateException("Started or completed examinations require a separately defined exceptional process.");
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM attendance WHERE exam_session_id=?) OR EXISTS(SELECT 1 FROM incident WHERE exam_session_id=?) OR EXISTS(SELECT 1 FROM generated_report WHERE exam_session_id=?)",Boolean.class,exam,exam,exam)))
            throw new IllegalStateException("Operational records block routine amendment; history will not be rewritten.");
        return row;
    }
    @Transactional
    public Map<String,Object> propose(int period,Proposal request) {
        lock.acquire();access.coordinator(period);
        var exam=requireFutureExam(period,request.examSessionId(),request.revision());
        long duration=Duration.between(((Time)exam.get("start_time")).toLocalTime(),((Time)exam.get("end_time")).toLocalTime()).toMinutes();
        var end=request.examDate().atTime(request.startTime()).plusMinutes(duration);
        if(!end.toLocalDate().equals(request.examDate())) throw new IllegalArgumentException("Exam must fit within a daily slot.");
        var result=jdbc.queryForMap("""
                INSERT INTO examination_amendment(period_id,exam_session_id,proposed_by_staff_id,expected_revision,exam_date,start_time,end_time,reason)
                VALUES (?,?,?,?,?,?,?,?) RETURNING *
                """,period,request.examSessionId(),staff.requireCurrentStaff().getStaffId(),request.revision(),request.examDate(),request.startTime(),end.toLocalTime(),request.reason().trim());
        long id=((Number)result.get("amendment_id")).longValue();
        saveScope(id,request);
        validateScope(period,id);
        return amendment(period,id);
    }
    @Transactional
    public Map<String,Object> decide(int period,long id,Decision decision) {
        lock.acquire();access.lead();var a=amendment(period,id);
        if(!"PENDING".equals(a.get("status"))) throw new IllegalStateException("Amendment already reviewed. Reload its status.");
        if(Objects.equals(a.get("proposed_by_staff_id"),staff.requireCurrentStaff().getStaffId())) throw new IllegalStateException("A different lead administrator must review this proposal.");
        if("APPROVED".equals(decision.status())) {
            requireFutureExam(period,((Number)a.get("exam_session_id")).intValue(),((Number)a.get("expected_revision")).longValue());
            validateScope(period,id);
        }
        return jdbc.queryForMap("UPDATE examination_amendment SET status=?,decision=?,reviewed_by_staff_id=?,reviewed_at=CURRENT_TIMESTAMP WHERE amendment_id=? RETURNING *",
                decision.status(),decision.reason().trim(),staff.requireCurrentStaff().getStaffId(),id);
    }
    @Transactional
    public Map<String,Object> apply(int period,long id) {
        lock.acquire();access.coordinator(period);var a=amendment(period,id);
        if(!"APPROVED".equals(a.get("status"))) throw new IllegalStateException("Only an approved unapplied amendment can be applied. Check its current status before retrying.");
        int exam=((Number)a.get("exam_session_id")).intValue();
        var current=requireFutureExam(period,exam,((Number)a.get("expected_revision")).longValue());
        if(!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM scheduling_lead_permission l JOIN staff s USING(staff_id)
                JOIN staff_role sr USING(staff_id) JOIN role r USING(role_id) WHERE l.staff_id=? AND s.account_status='ACTIVE' AND r.name='ADMINISTRATOR')
                """,Boolean.class,a.get("reviewed_by_staff_id")))) throw new IllegalStateException("Reviewer no longer holds active lead permission. Submit a new proposal.");
        var date=((Date)a.get("exam_date")).toLocalDate();var start=((Time)a.get("start_time")).toLocalTime();var end=((Time)a.get("end_time")).toLocalTime();
        validateScope(period,id);
        snapshot(id,exam,"before_arrangement");
        jdbc.queryForObject("SELECT set_config('app.scheduling_amendment',?,true)",String.class,Long.toString(id));
        jdbc.update("DELETE FROM invigilator_assignment WHERE exam_session_id=?",exam);
        jdbc.update("UPDATE exam_session SET exam_date=?,start_time=?,end_time=? WHERE exam_session_id=?",date,start,end,exam);
        jdbc.update("INSERT INTO exam_venue(exam_session_id,venue_id) SELECT ?,venue_id FROM examination_amendment_venue WHERE amendment_id=? ON CONFLICT DO NOTHING",exam,id);
        jdbc.update("""
                UPDATE student_venue_allocation a SET venue_id=p.venue_id FROM examination_amendment_allocation p
                WHERE p.amendment_id=? AND a.exam_session_id=? AND a.computer_number=p.computer_number AND a.venue_id<>p.venue_id
                """,id,exam);
        jdbc.update("DELETE FROM exam_venue v WHERE v.exam_session_id=? AND NOT EXISTS(SELECT 1 FROM examination_amendment_venue p WHERE p.amendment_id=? AND p.venue_id=v.venue_id)",exam,id);
        jdbc.update("""
                INSERT INTO invigilator_assignment(exam_session_id,venue_id,staff_id,assignment_status,published_at)
                SELECT ?,venue_id,staff_id,'PUBLISHED',? FROM examination_amendment_duty WHERE amendment_id=?
                """,exam,LocalDateTime.now(ZoneId.of((String)current.get("timezone"))),id);
        var validation=scheduling.validate(period);
        if(!validation.valid()) throw new IllegalStateException("Amendment blocked: "+String.join("; ",validation.problems()));
        jdbc.update("UPDATE examination_period SET revision=revision+1 WHERE period_id=?",period);
        snapshot(id,exam,"after_arrangement");
        jdbc.update("UPDATE examination_amendment SET status='APPLIED',applied_at=CURRENT_TIMESTAMP WHERE amendment_id=?",id);
        String message="Published examination "+current.get("course_code")+" (#"+exam+") moved to "+date+" "+start+"–"+end+" "+current.get("timezone")+". Review the updated venue allocations and invigilator duties in your examination details. Reason: "+a.get("reason");
        jdbc.update("""
                INSERT INTO examination_notification(amendment_id,recipient_kind,recipient_id,message)
                SELECT ?,'STUDENT',computer_number,? FROM student_venue_allocation WHERE exam_session_id=?
                ON CONFLICT DO NOTHING
                """,id,message,exam);
        jdbc.update("""
                INSERT INTO examination_notification(amendment_id,recipient_kind,recipient_id,message)
                SELECT ?,'STAFF',staff_id::text,? FROM (
                    SELECT staff_id FROM invigilator_assignment WHERE exam_session_id=? AND assignment_status='PUBLISHED'
                    UNION SELECT staff_id FROM course_lecturer WHERE course_code=?
                    UNION SELECT (old_duty->>'staff_id')::integer FROM examination_amendment a,
                        jsonb_array_elements(a.before_arrangement->'assignments') old_duty WHERE a.amendment_id=?) recipients
                ON CONFLICT DO NOTHING
                """,id,message,exam,current.get("course_code"),id);
        events.publishEvent(new SchedulingNotificationService.Applied(id));
        return amendment(period,id);
    }
    private Map<String,Object> withScope(Map<String,Object> row) {
        long id=((Number)row.get("amendment_id")).longValue();
        row.put("venueIds",jdbc.queryForList("SELECT venue_id FROM examination_amendment_venue WHERE amendment_id=? ORDER BY venue_id",Integer.class,id));
        row.put("duties",jdbc.queryForList("SELECT venue_id,staff_id FROM examination_amendment_duty WHERE amendment_id=? ORDER BY venue_id,staff_id",id));
        row.put("allocations",jdbc.queryForList("SELECT computer_number,venue_id FROM examination_amendment_allocation WHERE amendment_id=? ORDER BY computer_number",id));
        return row;
    }

    private void saveScope(long id,Proposal request) {
        var venues=request.venueIds()==null ? jdbc.queryForList("SELECT venue_id FROM exam_venue WHERE exam_session_id=? ORDER BY venue_id",Integer.class,request.examSessionId()) : request.venueIds();
        if(venues.isEmpty() || new HashSet<>(venues).size()!=venues.size()) throw new IllegalArgumentException("Select unique examination venues.");
        var capacity=new TreeMap<Integer,Integer>();
        for(int venue:venues) {
            var rooms=jdbc.queryForList("SELECT examination_capacity FROM venue WHERE venue_id=? AND examination_capacity>0",venue);
            if(rooms.isEmpty()) throw new IllegalArgumentException("Venue "+venue+" has no verified examination capacity.");
            capacity.put(venue,((Number)rooms.getFirst().get("examination_capacity")).intValue());
            jdbc.update("INSERT INTO examination_amendment_venue VALUES (?,?)",id,venue);
        }
        var allocated=jdbc.queryForList("SELECT computer_number,venue_id FROM student_venue_allocation WHERE exam_session_id=? ORDER BY computer_number",request.examSessionId());
        List<String> displaced=new ArrayList<>();
        // Keep every existing allocation whose venue remains selected. Only displaced students move.
        for(var a:allocated) {
            int venue=((Number)a.get("venue_id")).intValue();String student=(String)a.get("computer_number");
            if(capacity.containsKey(venue)) {
                if(capacity.get(venue)<=0) throw new IllegalStateException("Existing allocation exceeds verified capacity.");
                jdbc.update("INSERT INTO examination_amendment_allocation VALUES (?,?,?)",id,student,venue);
                capacity.computeIfPresent(venue,(v,n)->n-1);
            } else displaced.add(student);
        }
        for(String student:displaced) {
            int venue=capacity.entrySet().stream().filter(e->e.getValue()>0).map(Map.Entry::getKey).findFirst()
                    .orElseThrow(()->new IllegalStateException("Proposed venues cannot seat every registered student."));
            jdbc.update("INSERT INTO examination_amendment_allocation VALUES (?,?,?)",id,student,venue);
            capacity.computeIfPresent(venue,(v,n)->n-1);
        }
        var duties=request.duties()==null ? jdbc.query("SELECT venue_id,staff_id FROM invigilator_assignment WHERE exam_session_id=? AND assignment_status='PUBLISHED' ORDER BY venue_id,staff_id",(r,n)->new Duty(r.getInt(1),r.getInt(2)),request.examSessionId()) : request.duties();
        var staffIds=new HashSet<Integer>();
        for(var duty:duties) {
            if(!venues.contains(duty.venueId())) throw new IllegalArgumentException("Supply duties for the proposed venues; old venue duties cannot be silently moved.");
            if(!staffIds.add(duty.staffId())) throw new IllegalArgumentException("An invigilator cannot supervise two simultaneous venues.");
            jdbc.update("INSERT INTO examination_amendment_duty VALUES (?,?,?)",id,duty.venueId(),duty.staffId());
        }
    }

    private void validateScope(int period,long id) {
        var a=amendment(period,id);int exam=((Number)a.get("exam_session_id")).intValue();
        var date=((Date)a.get("exam_date")).toLocalDate();var start=((Time)a.get("start_time")).toLocalTime();var end=((Time)a.get("end_time")).toLocalTime();
        var venues=jdbc.queryForList("SELECT venue_id FROM examination_amendment_venue WHERE amendment_id=? ORDER BY venue_id",Integer.class,id);
        scheduling.validateAmendmentPlacement(period,exam,date,start,end,venues);
        if(Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM student_venue_allocation a WHERE a.exam_session_id=? AND NOT EXISTS(
                  SELECT 1 FROM examination_amendment_allocation p WHERE p.amendment_id=? AND p.computer_number=a.computer_number))
                OR EXISTS(SELECT 1 FROM examination_amendment_allocation p WHERE p.amendment_id=? AND NOT EXISTS(
                  SELECT 1 FROM student_venue_allocation a WHERE a.exam_session_id=? AND a.computer_number=p.computer_number))
                """,Boolean.class,exam,id,id,exam))) throw new IllegalStateException("Allocation membership changed since the amendment was proposed.");
        for(int venue:venues) {
            Long count=jdbc.queryForObject("SELECT count(*) FROM examination_amendment_allocation WHERE amendment_id=? AND venue_id=?",Long.class,id,venue);
            Integer cap=jdbc.queryForObject("SELECT examination_capacity FROM venue WHERE venue_id=?",Integer.class,venue);
            if(cap==null || count>cap) throw new IllegalStateException("Proposed allocations exceed current capacity in venue "+venue);
            Long assigned=jdbc.queryForObject("""
                    SELECT count(*) FROM examination_amendment_duty d JOIN staff s USING(staff_id)
                    WHERE d.amendment_id=? AND d.venue_id=? AND s.account_status='ACTIVE'
                    AND EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=d.staff_id AND r.name='INVIGILATOR')
                    """,Long.class,id,venue);
            if(assigned<Math.max(1,(count+49)/50)) throw new IllegalStateException("Proposed venue "+venue+" is understaffed.");
        }
        if(Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM examination_amendment_duty d JOIN staff s USING(staff_id) WHERE d.amendment_id=?
                    AND (s.account_status<>'ACTIVE' OR NOT EXISTS(SELECT 1 FROM staff_role sr JOIN role r USING(role_id) WHERE sr.staff_id=s.staff_id AND r.name='INVIGILATOR')))
                OR EXISTS(SELECT 1 FROM examination_amendment_duty d JOIN invigilator_assignment b USING(staff_id)
                    JOIN exam_session e USING(exam_session_id) WHERE d.amendment_id=? AND b.exam_session_id<>? AND b.assignment_status<>'CANCELLED'
                    AND e.exam_date+e.start_time<? AND e.exam_date+e.end_time>?)
                """,Boolean.class,id,id,exam,date.atTime(end),date.atTime(start)))) throw new IllegalStateException("Proposed invigilators are inactive, unauthorised or have overlapping duties.");
    }

    private void snapshot(long id,int exam,String column) {
        // column is an internal constant, never request input.
        jdbc.update("UPDATE examination_amendment SET "+column+"=jsonb_build_object("+
                "'exam',(SELECT to_jsonb(e) FROM exam_session e WHERE exam_session_id=?),"+
                "'bookings',(SELECT COALESCE(jsonb_agg(to_jsonb(v)),'[]'::jsonb) FROM exam_venue v WHERE exam_session_id=?),"+
                "'allocations',(SELECT COALESCE(jsonb_agg(to_jsonb(a)),'[]'::jsonb) FROM student_venue_allocation a WHERE exam_session_id=?),"+
                "'assignments',(SELECT COALESCE(jsonb_agg(to_jsonb(d)),'[]'::jsonb) FROM invigilator_assignment d WHERE exam_session_id=?)) WHERE amendment_id=?",exam,exam,exam,exam,id);
    }

    @Transactional
    public List<Map<String,Object>> retryNotifications(int period,long id) {
        lock.acquire();access.coordinator(period);
        if(!"APPLIED".equals(amendment(period,id).get("status"))) throw new IllegalStateException("Notifications require an applied amendment.");
        try { notifications.deliver(id); }
        catch(RuntimeException ex) {
            try { notifications.failed(id); } catch(RuntimeException unavailable) { /* PENDING remains retryable when the database is unavailable. */ }
            throw ex;
        }
        return notifications(period,id);
    }

    public List<Map<String,Object>> notifications(int period,long id) {
        amendment(period,id);
        return jdbc.queryForList("SELECT * FROM examination_notification WHERE amendment_id=? ORDER BY notification_id",id);
    }
}

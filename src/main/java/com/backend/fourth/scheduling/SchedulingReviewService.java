package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.repository.AssignmentWriteLock;
import com.backend.fourth.staff.entity.Staff;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class SchedulingReviewService {
    private final JdbcTemplate jdbc;
    private final CurrentStaffResolver currentStaff;
    private final SchedulingAccess access;
    private final SchedulingDeniedAudit denied;
    private final AssignmentWriteLock lock;

    public record Change(@Positive int periodId,@NotBlank @Size(max=15) String courseCode,
                         @Positive Integer examSessionId,@NotBlank @Size(max=4000) String proposedChange,
                         @NotBlank @Size(max=2000) String reason) {}
    public record Decision(@NotBlank @Pattern(regexp="APPROVED|REJECTED") String status,
                           @NotBlank @Size(max=2000) String decision) {}
    public record CapacityDraft(@Positive int periodId,@NotBlank @Size(max=15) String courseCode,
                                @Positive int venueId,@Positive int lecturerStaffId) {}

    private boolean administrator(Staff staff) { return staff.getRoles().stream().anyMatch(r->"ADMINISTRATOR".equals(r.getName())); }
    private Staff actor() {
        var actor=currentStaff.requireCurrentStaff();
        if(!"ACTIVE".equals(actor.getAccountStatus())) throw new AccessDeniedException("An active staff account is required.");
        return actor;
    }
    private void requireCourse(int period,String course,Staff actor) {
        boolean allowed=Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM examination_period_course pc WHERE pc.period_id=? AND pc.course_code=?
                AND (? OR (EXISTS(SELECT 1 FROM course_lecturer cl WHERE cl.course_code=pc.course_code AND cl.staff_id=?)
                AND EXISTS(SELECT 1 FROM exam_session e WHERE e.period_id=pc.period_id AND e.course_code=pc.course_code AND e.schedule_published))))
                """,Boolean.class,period,course,administrator(actor),actor.getStaffId()));
        if(!allowed) {
            denied.record(actor.getStaffId(),period);
            throw new AccessDeniedException("You cannot access this period/course change request.");
        }
    }

    @Transactional
    public Map<String,Object> submit(Change request) {
        lock.acquire();var actor=actor();requireCourse(request.periodId(),request.courseCode(),actor);
        if(request.examSessionId()!=null && !Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM exam_session WHERE exam_session_id=? AND period_id=? AND course_code=?)",Boolean.class,request.examSessionId(),request.periodId(),request.courseCode()))) {
            denied.record(actor.getStaffId(),request.periodId());
            throw new AccessDeniedException("You cannot access this period/course examination.");
        }
        jdbc.queryForObject("SELECT set_config('app.scheduling_actor',?,true)",String.class,actor.getStaffId().toString());
        return jdbc.queryForMap("""
                INSERT INTO examination_change_request(requester_staff_id,period_id,course_code,exam_session_id,proposed_change,reason)
                VALUES (?,?,?,?,?,?) RETURNING *
                """,actor.getStaffId(),request.periodId(),request.courseCode(),request.examSessionId(),request.proposedChange().trim(),request.reason().trim());
    }
    public List<Map<String,Object>> list(int period) {
        var actor=actor();
        if(administrator(actor)) return jdbc.queryForList("SELECT * FROM examination_change_request WHERE period_id=? ORDER BY request_id DESC",period);
        return jdbc.queryForList("""
                SELECT q.* FROM examination_change_request q WHERE q.period_id=? AND q.requester_staff_id=?
                AND EXISTS(SELECT 1 FROM course_lecturer c WHERE c.course_code=q.course_code AND c.staff_id=?)
                AND EXISTS(SELECT 1 FROM exam_session e WHERE e.period_id=q.period_id AND e.course_code=q.course_code AND e.schedule_published)
                ORDER BY q.request_id DESC
                """,period,actor.getStaffId(),actor.getStaffId());
    }
    @Transactional
    public Map<String,Object> decide(long id,Decision decision) {
        lock.acquire();access.administrator();
        var rows=jdbc.queryForList("SELECT * FROM examination_change_request WHERE request_id=?",id);
        if(rows.isEmpty()) throw new IllegalArgumentException("Change request not found.");
        var row=rows.getFirst();
        if(!"PENDING".equals(row.get("status"))) throw new IllegalStateException("This request has already been decided. Reload its status.");
        return jdbc.queryForMap("""
                UPDATE examination_change_request SET status=?,decision=?,decided_by_staff_id=?,decided_at=CURRENT_TIMESTAMP
                WHERE request_id=? RETURNING *
                """,decision.status(),decision.decision().trim(),actor().getStaffId(),id);
    }
    public List<Map<String,Object>> lecturers(int period,String course) {
        var actor=actor(); if(!administrator(actor)) throw new AccessDeniedException("Administrator access required.");
        requireCourse(period,course,actor);
        return jdbc.queryForList("SELECT s.staff_id,s.full_name,s.email FROM course_lecturer c JOIN staff s USING(staff_id) WHERE c.course_code=? ORDER BY s.staff_id",course);
    }
    public Map<String,Object> capacityDraft(CapacityDraft request) {
        var lecturers=lecturers(request.periodId(),request.courseCode());
        var lecturer=lecturers.stream().filter(s->((Number)s.get("staff_id")).intValue()==request.lecturerStaffId()).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Choose a lecturer assigned to this course; no address was guessed."));
        var venues=jdbc.queryForList("SELECT venue_name FROM venue WHERE venue_id=?",request.venueId());
        if(venues.isEmpty()) throw new IllegalArgumentException("Venue not found.");
        String address=(String)lecturer.get("email");
        if(address==null || address.isBlank()) throw new IllegalStateException("Assigned lecturer has no email address. Correct the staff record before preparing an addressed draft.");
        String venue=(String)venues.getFirst().get("venue_name");
        return Map.of("to",address,"subject","Request verified examination capacity: "+venue,
                "body","Dear "+lecturer.get("full_name")+",\n\nPlease confirm the verified examination seating capacity of "+venue
                        +" (venue #"+request.venueId()+") for "+request.courseCode()+". Please distinguish examination seating from classroom capacity and confirm any physically arranged extra seats.\n\nThank you.",
                "deliveryStatus","DRAFT_ONLY","sendAvailable",false,
                "message","No application email sender is configured. Review and copy this draft; no email has been sent and capacity has not changed.");
    }
}

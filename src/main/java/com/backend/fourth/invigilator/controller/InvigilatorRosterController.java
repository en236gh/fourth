package com.backend.fourth.invigilator.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invigilator/assignments")
@RequiredArgsConstructor
public class InvigilatorRosterController {
    private final JdbcTemplate jdbc;
    private final CurrentStaffResolver staff;

    @GetMapping("/{exam}/{venue}/students")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    @Transactional(readOnly=true)
    public ApiResponse<?> students(@PathVariable int exam,@PathVariable int venue) {
        int staffId=staff.requireCurrentStaff().getStaffId();
        if (!Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM invigilator_assignment a JOIN exam_session e USING(exam_session_id)
                WHERE a.exam_session_id=? AND a.venue_id=? AND a.staff_id=? AND a.assignment_status='PUBLISHED' AND e.schedule_published)
                """,Boolean.class,exam,venue,staffId))) throw new AccessDeniedException("You are not assigned to this published examination venue");
        return ApiResponse.success("Allocated examination students",jdbc.queryForList("""
                SELECT a.computer_number,s.full_name,a.exam_session_id,a.venue_id,att.attendance_status
                FROM student_venue_allocation a JOIN student s USING(computer_number) JOIN exam_session e USING(exam_session_id)
                JOIN student_registration r ON r.computer_number=a.computer_number AND r.course_code=e.course_code AND r.academic_year=e.academic_year AND r.semester=e.semester
                LEFT JOIN attendance att ON att.computer_number=a.computer_number AND att.exam_session_id=a.exam_session_id
                WHERE a.exam_session_id=? AND a.venue_id=? ORDER BY a.computer_number
                """,exam,venue));
    }
}

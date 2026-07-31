package com.backend.fourth.attendance.entity;

import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.venue.entity.Venue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Integer attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computer_number", referencedColumnName = "computer_number")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_in_venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_staff_id", nullable = false)
    private Staff verifiedBy;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_method", nullable = false)
    private VerificationMethod verificationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false)
    private AttendanceStatus attendanceStatus;

    @Column(name = "scripts_submitted")
    private Boolean scriptsSubmitted = false;

    @Column(name = "alert_message")
    private String alertMessage;
}

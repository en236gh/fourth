package com.backend.fourth.exam.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "exam_session")
@Getter
@Setter
public class ExamSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_session_id")
    private Integer examSessionId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "status", nullable = false)
    private String status = "SCHEDULED";
}

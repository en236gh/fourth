package com.backend.fourth.report.entity;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.staff.entity.Staff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_report")
@Getter
@Setter
public class GeneratedReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by_staff_id")
    private Staff generatedBy;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "report_type", nullable = false)
    private String reportType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "summary")
    private String summary;
}

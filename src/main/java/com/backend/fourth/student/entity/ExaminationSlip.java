package com.backend.fourth.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "examination_slip",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_examination_slip_student_exam",
                columnNames = {"computer_number", "exam_session_id"}
        )
)
@Getter
@Setter
public class ExaminationSlip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slip_id")
    private Long slipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "computer_number", nullable = false)
    private Student student;

    @Column(name = "exam_session_id", nullable = false)
    private Integer examSessionId;

    @Column(name = "qr_token", nullable = false, columnDefinition = "TEXT")
    private String qrToken;

    @Column(name = "qr_jti", nullable = false, unique = true, length = 64)
    private String qrJti;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}

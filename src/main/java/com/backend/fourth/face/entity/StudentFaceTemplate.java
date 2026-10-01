package com.backend.fourth.face.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** A student's enrolled face embedding. The enrolment photo itself is not stored. */
@Entity
@Table(name = "student_face_template")
@Getter
@Setter
public class StudentFaceTemplate {
    @Id
    @Column(name = "computer_number", nullable = false)
    private String computerNumber;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", nullable = false, columnDefinition = "real[]")
    private float[] embedding;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "detection_score", nullable = false)
    private Float detectionScore;

    @Column(name = "consent_recorded_at", nullable = false)
    private LocalDateTime consentRecordedAt;

    @Column(name = "enrolled_by_staff_id")
    private Integer enrolledByStaffId;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

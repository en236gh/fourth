package com.backend.fourth.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "course_lecturer")
@IdClass(CourseLecturerId.class)
@Getter
@Setter
public class CourseLecturer {
    @Id
    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Id
    @Column(name = "staff_id", nullable = false)
    private Integer staffId;
}

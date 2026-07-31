package com.backend.fourth.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "student_registration")
@IdClass(StudentRegistrationId.class)
@Getter
@Setter
public class StudentRegistration {
    @Id
    @Column(name = "computer_number", nullable = false)
    private String computerNumber;

    @Id
    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Id
    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Id
    @Column(name = "semester", nullable = false)
    private Integer semester;
}

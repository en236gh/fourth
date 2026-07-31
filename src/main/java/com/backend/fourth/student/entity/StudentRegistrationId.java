package com.backend.fourth.student.entity;

import java.io.Serializable;
import java.util.Objects;

public class StudentRegistrationId implements Serializable {
    private String computerNumber;
    private String courseCode;
    private String academicYear;
    private Integer semester;

    public StudentRegistrationId() {
    }

    public StudentRegistrationId(String computerNumber, String courseCode, String academicYear, Integer semester) {
        this.computerNumber = computerNumber;
        this.courseCode = courseCode;
        this.academicYear = academicYear;
        this.semester = semester;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StudentRegistrationId that)) {
            return false;
        }
        return Objects.equals(computerNumber, that.computerNumber)
                && Objects.equals(courseCode, that.courseCode)
                && Objects.equals(academicYear, that.academicYear)
                && Objects.equals(semester, that.semester);
    }

    @Override
    public int hashCode() {
        return Objects.hash(computerNumber, courseCode, academicYear, semester);
    }
}

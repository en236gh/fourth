package com.backend.fourth.exam.entity;

import java.io.Serializable;
import java.util.Objects;

public class CourseLecturerId implements Serializable {
    private String courseCode;
    private Integer staffId;

    public CourseLecturerId() {
    }

    public CourseLecturerId(String courseCode, Integer staffId) {
        this.courseCode = courseCode;
        this.staffId = staffId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CourseLecturerId that)) {
            return false;
        }
        return Objects.equals(courseCode, that.courseCode)
                && Objects.equals(staffId, that.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseCode, staffId);
    }
}

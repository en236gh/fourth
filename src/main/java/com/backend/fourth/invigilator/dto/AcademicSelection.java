package com.backend.fourth.invigilator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AcademicSelection(
        @NotNull @Positive Integer schoolId,
        @NotNull @Positive Integer programmeId,
        @NotNull @Positive Integer yearOfStudy,
        @NotBlank String courseCode,
        @Positive Integer majorId) {
    public AcademicSelection(Integer schoolId, Integer programmeId, Integer yearOfStudy, String courseCode) {
        this(schoolId, programmeId, yearOfStudy, courseCode, null);
    }
}

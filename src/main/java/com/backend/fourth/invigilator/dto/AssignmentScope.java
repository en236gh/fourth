package com.backend.fourth.invigilator.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record AssignmentScope(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime timeTo,
        @Positive Integer schoolId,
        @Positive Integer programmeId,
        @Positive Integer yearOfStudy,
        String courseCode,
        String academicYear,
        @Positive Integer semester) {

    @JsonIgnore
    @AssertTrue(message = "dateTo must be on or after dateFrom")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateTo.isBefore(dateFrom);
    }

    @JsonIgnore
    @AssertTrue(message = "Provide both timeFrom and timeTo, with timeTo after timeFrom")
    public boolean isTimeRangeValid() {
        return timeFrom == null && timeTo == null
                || timeFrom != null && timeTo != null && timeTo.isAfter(timeFrom);
    }

    @JsonIgnore
    @AssertTrue(message = "courseCode and academicYear must not be blank when supplied")
    public boolean isTextValid() {
        return (courseCode == null || !courseCode.isBlank()) && (academicYear == null || !academicYear.isBlank());
    }
}

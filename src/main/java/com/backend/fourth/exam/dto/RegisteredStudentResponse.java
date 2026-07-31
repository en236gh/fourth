package com.backend.fourth.exam.dto;

public record RegisteredStudentResponse(
        String computerNumber,
        String fullName,
        String program,
        Integer yearOfStudy,
        String photoPath,
        String status
) {
}

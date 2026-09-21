package com.backend.fourth.invigilator.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.invigilator.dto.AcademicSelection;
import com.backend.fourth.invigilator.repository.AssignmentAcademicRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/invigilator-assignments/academics")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
@RequiredArgsConstructor
public class AssignmentAcademicController {
    private final AssignmentAcademicRepository repository;

    @GetMapping("/schools")
    public ApiResponse<List<Map<String, Object>>> schools() {
        return ApiResponse.success("Schools retrieved", repository.schools());
    }

    @GetMapping("/programmes")
    public ApiResponse<List<Map<String, Object>>> programmes(@RequestParam int schoolId) {
        return ApiResponse.success("Programmes retrieved", repository.programmes(schoolId));
    }

    @GetMapping("/years")
    public ApiResponse<List<Map<String, Object>>> years(@RequestParam int schoolId, @RequestParam int programmeId) {
        return ApiResponse.success("Years of study retrieved", repository.years(schoolId, programmeId));
    }

    @GetMapping("/courses")
    public ApiResponse<List<Map<String, Object>>> courses(@RequestParam int schoolId,
            @RequestParam int programmeId, @RequestParam int yearOfStudy) {
        return ApiResponse.success("Courses retrieved", repository.courses(schoolId, programmeId, yearOfStudy));
    }

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams(@Valid @ModelAttribute AcademicSelection selection) {
        return ApiResponse.success("Exams retrieved", repository.exams(selection));
    }
}

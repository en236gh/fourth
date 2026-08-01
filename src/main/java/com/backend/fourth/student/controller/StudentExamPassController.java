package com.backend.fourth.student.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStudentResolver;
import com.backend.fourth.student.dto.ExaminationPassResponse;
import com.backend.fourth.student.dto.StudentExaminationSummaryResponse;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.service.StudentExamPassService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentExamPassController {
    private final StudentExamPassService studentExamPassService;
    private final CurrentStudentResolver currentStudentResolver;

    @GetMapping("/examinations")
    public ApiResponse<List<StudentExaminationSummaryResponse>> listExaminations() {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examinations retrieved",
                studentExamPassService.listMyExaminations(student));
    }

    @PostMapping("/examination-pass")
    public ApiResponse<ExaminationPassResponse> generatePass(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester) {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examination pass generated",
                studentExamPassService.generatePass(student, academicYear, semester));
    }

    @GetMapping("/examination-pass")
    public ApiResponse<ExaminationPassResponse> getPass(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester) {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examination pass retrieved",
                studentExamPassService.getPass(student, academicYear, semester));
    }

    @GetMapping("/examination-pass/pdf")
    public ResponseEntity<byte[]> downloadPassPdf(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester) {
        Student student = currentStudentResolver.requireCurrentStudent();
        byte[] pdf = studentExamPassService.downloadPassPdf(student, academicYear, semester);
        String filename = "exam-pass-" + student.getComputerNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}

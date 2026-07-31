package com.backend.fourth.student.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStudentResolver;
import com.backend.fourth.student.dto.ExaminationSlipResponse;
import com.backend.fourth.student.dto.StudentExaminationSummaryResponse;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.service.StudentExamSlipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentExamSlipController {
    private final StudentExamSlipService studentExamSlipService;
    private final CurrentStudentResolver currentStudentResolver;

    @GetMapping("/examinations")
    public ApiResponse<List<StudentExaminationSummaryResponse>> listExaminations() {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examinations retrieved",
                studentExamSlipService.listMyExaminations(student));
    }

    @PostMapping("/examinations/{examSessionId}/slip")
    public ApiResponse<ExaminationSlipResponse> generateSlip(@PathVariable Integer examSessionId) {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examination slip generated",
                studentExamSlipService.generateSlip(student, examSessionId));
    }

    @GetMapping("/examinations/{examSessionId}/slip")
    public ApiResponse<ExaminationSlipResponse> getSlip(@PathVariable Integer examSessionId) {
        Student student = currentStudentResolver.requireCurrentStudent();
        return ApiResponse.success(
                "Examination slip retrieved",
                studentExamSlipService.getSlip(student, examSessionId));
    }

    @GetMapping("/examinations/{examSessionId}/slip/pdf")
    public ResponseEntity<byte[]> downloadSlipPdf(@PathVariable Integer examSessionId) {
        Student student = currentStudentResolver.requireCurrentStudent();
        byte[] pdf = studentExamSlipService.downloadSlipPdf(student, examSessionId);
        String filename = "exam-slip-" + student.getComputerNumber() + "-" + examSessionId + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}

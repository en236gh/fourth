package com.backend.fourth.face.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.face.dto.FaceEnrolmentResponse;
import com.backend.fourth.face.service.FaceEnrolmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/students/{computerNumber}/face")
@RequiredArgsConstructor
public class FaceEnrolmentController {
    private final FaceEnrolmentService faceEnrolmentService;
    private final CurrentStaffResolver currentStaffResolver;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<FaceEnrolmentResponse> enrol(
            @PathVariable String computerNumber,
            @RequestPart("image") MultipartFile image,
            @RequestParam(defaultValue = "false") boolean consentConfirmed) throws IOException {
        return ApiResponse.success("Face enrolled", faceEnrolmentService.enrol(
                computerNumber, image.getBytes(), image.getContentType(), consentConfirmed,
                currentStaffResolver.requireCurrentStaff()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<FaceEnrolmentResponse> status(@PathVariable String computerNumber) {
        return ApiResponse.success("Face enrolment status", faceEnrolmentService.status(computerNumber));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<FaceEnrolmentResponse> remove(@PathVariable String computerNumber) {
        return ApiResponse.success("Face enrolment removed", faceEnrolmentService.remove(computerNumber));
    }
}

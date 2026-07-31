package com.backend.fourth.invigilator.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.dto.InvigilatorAssignmentResponse;
import com.backend.fourth.invigilator.service.InvigilatorService;
import com.backend.fourth.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invigilator")
@RequiredArgsConstructor
public class InvigilatorController {
    private final InvigilatorService invigilatorService;
    private final CurrentStaffResolver currentStaffResolver;

    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<List<InvigilatorAssignmentResponse>> myAssignments() {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Assignments retrieved", invigilatorService.myAssignments(staff));
    }

    @PostMapping("/assignments/{examSessionId}/{venueId}/start")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<InvigilatorAssignmentResponse> startSession(
            @PathVariable Integer examSessionId,
            @PathVariable Integer venueId) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success(
                "Examination session started",
                invigilatorService.startSession(staff, examSessionId, venueId));
    }

    @PostMapping("/assignments/{examSessionId}/{venueId}/end")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<InvigilatorAssignmentResponse> endSession(
            @PathVariable Integer examSessionId,
            @PathVariable Integer venueId) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success(
                "Examination session ended",
                invigilatorService.endSession(staff, examSessionId, venueId));
    }
}

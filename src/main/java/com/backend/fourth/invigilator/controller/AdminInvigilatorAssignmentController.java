package com.backend.fourth.invigilator.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.dto.AdminAssignmentResponse;
import com.backend.fourth.invigilator.dto.AcademicSelection;
import com.backend.fourth.invigilator.dto.AdminStaffingResponse;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.dto.CreateInvigilatorAssignmentRequest;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/invigilator-assignments")
@RequiredArgsConstructor
public class AdminInvigilatorAssignmentController {
    private final AdminInvigilatorAssignmentService service;
    private final CurrentStaffResolver currentStaffResolver;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<AdminAssignmentResponse>> list(
            @RequestParam(required = false) Integer examSessionId) {
        return ApiResponse.success("Invigilator assignments retrieved", service.list(examSessionId));
    }

    @GetMapping("/exam-sessions/{examSessionId}/staffing")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<AdminStaffingResponse>> staffing(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Exam venue staffing retrieved", service.staffing(examSessionId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<AdminAssignmentResponse> create(@Valid @RequestBody CreateInvigilatorAssignmentRequest request) {
        return ApiResponse.success("Draft invigilator assignment created", service.createDraft(request, currentStaffResolver.requireCurrentStaff()));
    }
    @PostMapping("/exam-sessions/{examSessionId}/auto-assign")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<AutoAssignmentResponse> autoAssign(@PathVariable Integer examSessionId,
            @Valid @RequestBody AcademicSelection selection) {
        return ApiResponse.success("Draft assignments generated", service.autoAssignDrafts(examSessionId, selection, currentStaffResolver.requireCurrentStaff()));
    }
    @PostMapping("/exam-sessions/{examSessionId}/publish")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<AdminAssignmentResponse>> publish(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Draft assignments published", service.publish(examSessionId));
    }

    @PostMapping("/{examSessionId}/{venueId}/{staffId}/cancel")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<AdminAssignmentResponse> cancel(
            @PathVariable Integer examSessionId,
            @PathVariable Integer venueId,
            @PathVariable Integer staffId) {
        return ApiResponse.success("Invigilator assignment cancelled",
                service.cancel(examSessionId, venueId, staffId));
    }
}

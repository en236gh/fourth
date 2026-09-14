package com.backend.fourth.staff.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.staff.dto.CreateStaffRequest;
import com.backend.fourth.staff.dto.CreateStaffResponse;
import com.backend.fourth.staff.service.StaffAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class StaffAdminController {
    private final StaffAdminService staffAdminService;

    @PostMapping("/staff")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<CreateStaffResponse>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(staffAdminService.createStaff(request));
    }
}

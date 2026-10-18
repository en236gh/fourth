package com.backend.fourth.attendance.controller;

import com.backend.fourth.attendance.service.OfflineAttendanceService;
import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@PreAuthorize("hasAuthority('INVIGILATOR')")
@RequiredArgsConstructor
public class OfflineAttendanceController {
    private final OfflineAttendanceService service;
    private final CurrentStaffResolver staff;

    public record SyncRequest(List<Object> scans) {}

    @GetMapping("/offline-exam-data")
    public ResponseEntity<ApiResponse<OfflineAttendanceService.Snapshot>> download() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                ApiResponse.success("Offline examination snapshot", service.download(staff.requireCurrentStaff())));
    }

    @PostMapping("/sync")
    public ApiResponse<List<OfflineAttendanceService.Result>> sync(@RequestBody SyncRequest request) {
        return ApiResponse.success("Attendance sync results", service.sync(request.scans(), staff.requireCurrentStaff()));
    }
}

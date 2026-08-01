package com.backend.fourth.attendance.controller;

import com.backend.fourth.attendance.dto.AttendanceCheckInResponse;
import com.backend.fourth.attendance.dto.AttendanceSummaryResponse;
import com.backend.fourth.attendance.dto.CheckInRequest;
import com.backend.fourth.attendance.dto.QrCheckInRequest;
import com.backend.fourth.attendance.dto.QrLookupRequest;
import com.backend.fourth.attendance.dto.ScriptsCollectedRequest;
import com.backend.fourth.attendance.dto.StudentLookupResponse;
import com.backend.fourth.attendance.service.AttendanceService;
import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.staff.entity.Staff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final AttendanceService attendanceService;
    private final CurrentStaffResolver currentStaffResolver;

    @GetMapping("/lookup")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<StudentLookupResponse> lookup(
            @RequestParam String computerNumber,
            @RequestParam Integer examSessionId) {
        Staff invigilator = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success(
                "Student retrieved",
                attendanceService.lookupStudent(computerNumber, examSessionId, invigilator));
    }

    @PostMapping("/lookup-by-qr")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<StudentLookupResponse> lookupByQr(@Valid @RequestBody QrLookupRequest request) {
        Staff invigilator = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success(
                "Student retrieved from QR",
                attendanceService.lookupStudentByQr(request, invigilator));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<AttendanceCheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        Staff invigilator = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Attendance recorded", attendanceService.checkIn(request, invigilator));
    }

    @PostMapping("/check-in-by-qr")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<AttendanceCheckInResponse> checkInByQr(@Valid @RequestBody QrCheckInRequest request) {
        Staff invigilator = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success(
                "Attendance recorded from QR",
                attendanceService.checkInByQr(request, invigilator));
    }

    @GetMapping("/exam/{examSessionId}")
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR') or hasAuthority('LECTURER')")
    public ApiResponse<List<AttendanceCheckInResponse>> listByExam(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Attendance retrieved", attendanceService.getAttendanceForExam(examSessionId));
    }

    @PostMapping("/exam/{examSessionId}/scripts-collected")
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<AttendanceSummaryResponse> markScriptsCollected(
            @PathVariable Integer examSessionId,
            @Valid @RequestBody ScriptsCollectedRequest request) {
        return ApiResponse.success(
                "Scripts count updated",
                attendanceService.updateScriptsCollected(examSessionId, request.count()));
    }

    @GetMapping("/exam/{examSessionId}/summary")
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR') or hasAuthority('LECTURER')")
    public ApiResponse<AttendanceSummaryResponse> summary(@PathVariable Integer examSessionId) {
        return ApiResponse.success("Attendance summary retrieved", attendanceService.getAttendanceSummary(examSessionId));
    }
}

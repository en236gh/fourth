package com.backend.fourth.attendance.dto;

import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import com.backend.fourth.attendance.entity.VerificationMethod;

import java.time.LocalDateTime;

public record AttendanceCheckInResponse(
        Integer attendanceId,
        String computerNumber,
        String studentName,
        Integer examSessionId,
        Integer venueId,
        String venueName,
        Integer verifiedByStaffId,
        String verifiedByName,
        LocalDateTime checkInTime,
        VerificationMethod verificationMethod,
        AttendanceStatus attendanceStatus,
        Boolean scriptsSubmitted,
        String alertMessage
) {
    public static AttendanceCheckInResponse from(Attendance attendance) {
        return new AttendanceCheckInResponse(
                attendance.getAttendanceId(),
                attendance.getStudent() != null ? attendance.getStudent().getComputerNumber() : null,
                attendance.getStudent() != null ? attendance.getStudent().getFullName() : null,
                attendance.getExamSession() != null ? attendance.getExamSession().getExamSessionId() : null,
                attendance.getVenue() != null ? attendance.getVenue().getVenueId() : null,
                attendance.getVenue() != null ? attendance.getVenue().getVenueName() : null,
                attendance.getVerifiedBy() != null ? attendance.getVerifiedBy().getStaffId() : null,
                attendance.getVerifiedBy() != null ? attendance.getVerifiedBy().getFullName() : null,
                attendance.getCheckInTime(),
                attendance.getVerificationMethod(),
                attendance.getAttendanceStatus(),
                attendance.getScriptsSubmitted(),
                attendance.getAlertMessage()
        );
    }
}

package com.backend.fourth.attendance.repository;

import com.backend.fourth.attendance.entity.Attendance;
import com.backend.fourth.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    Optional<Attendance> findByStudentComputerNumberAndExamSessionExamSessionId(String computerNumber, Integer examSessionId);

    List<Attendance> findByExamSessionExamSessionId(Integer examSessionId);

    @Query("""
            SELECT a FROM Attendance a
            JOIN FETCH a.student
            JOIN FETCH a.examSession
            JOIN FETCH a.venue
            JOIN FETCH a.verifiedBy
            WHERE a.examSession.examSessionId = :examSessionId
            ORDER BY a.checkInTime ASC
            """)
    List<Attendance> findDetailedByExamSessionId(@Param("examSessionId") Integer examSessionId);

    long countByExamSessionExamSessionIdAndAttendanceStatus(Integer examSessionId, AttendanceStatus attendanceStatus);

    long countByAttendanceStatus(AttendanceStatus attendanceStatus);
}

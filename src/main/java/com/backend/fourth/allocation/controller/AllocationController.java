package com.backend.fourth.allocation.controller;

import com.backend.fourth.allocation.dto.AllocationStatsResponse;
import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.service.AllocationService;
import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/allocation")
@RequiredArgsConstructor
public class AllocationController {
    private final AllocationService allocationService;
    private final ExamSessionRepository examSessionRepository;

    @PostMapping("/exam-session/{examSessionId}")
    @PreAuthorize("hasAuthority('LECTURER')")
    public ApiResponse<List<StudentVenueAllocation>> autoAllocate(@PathVariable Integer examSessionId) {
        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        return ApiResponse.success("Allocations created", allocationService.allocateStudentsToVenues(examSession));
    }

    @GetMapping("/exam-session/{examSessionId}")
    @PreAuthorize("hasAuthority('LECTURER') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<AllocationStatsResponse> getAllocations(@PathVariable Integer examSessionId) {
        ExamSession examSession = examSessionRepository.findById(examSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        return ApiResponse.success("Allocation statistics retrieved", allocationService.getAllocationStats(examSession));
    }
}

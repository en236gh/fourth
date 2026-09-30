package com.backend.fourth.scheduling;

import com.backend.fourth.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/examination-change-requests")
@PreAuthorize("hasAnyAuthority('ADMINISTRATOR','LECTURER')")
public class SchedulingReviewController {
    private final SchedulingReviewService service;
    @PostMapping
    public ApiResponse<?> submit(@Valid @RequestBody SchedulingReviewService.Change request) { return ApiResponse.success("Request submitted; timetable unchanged",service.submit(request)); }
    @GetMapping
    public ApiResponse<?> list(@RequestParam int periodId) { return ApiResponse.success("Change requests",service.list(periodId)); }
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<?> decide(@PathVariable long id,@Valid @RequestBody SchedulingReviewService.Decision request) { return ApiResponse.success("Decision recorded; timetable unchanged",service.decide(id,request)); }
    @GetMapping("/capacity-lecturers")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<?> lecturers(@RequestParam int periodId,@RequestParam String courseCode) { return ApiResponse.success("Assigned lecturers",service.lecturers(periodId,courseCode)); }
    @PostMapping("/capacity-draft")
    @PreAuthorize("hasAuthority('ADMINISTRATOR')")
    public ApiResponse<?> draft(@Valid @RequestBody SchedulingReviewService.CapacityDraft request) { return ApiResponse.success("Reviewable email draft only",service.capacityDraft(request)); }
}

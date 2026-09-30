package com.backend.fourth.scheduling;

import com.backend.fourth.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/examination-periods/{period}/amendments")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
public class SchedulingAmendmentController {
    private final SchedulingAmendmentService service;
    @GetMapping public ApiResponse<?> list(@PathVariable int period) { return ApiResponse.success("Timetable amendments",service.list(period)); }
    @PostMapping public ApiResponse<?> propose(@PathVariable int period,@Valid @RequestBody SchedulingAmendmentService.Proposal request) { return ApiResponse.success("Amendment proposed; timetable unchanged",service.propose(period,request)); }
    @PostMapping("/{id}/decision") public ApiResponse<?> decide(@PathVariable int period,@PathVariable long id,@Valid @RequestBody SchedulingAmendmentService.Decision request) { return ApiResponse.success("Lead decision recorded",service.decide(period,id,request)); }
    @PostMapping("/{id}/apply") public ApiResponse<?> apply(@PathVariable int period,@PathVariable long id) { return ApiResponse.success("Approved amendment applied; inbox notifications queued",service.apply(period,id)); }
    @GetMapping("/{id}/notifications") public ApiResponse<?> notifications(@PathVariable int period,@PathVariable long id) { return ApiResponse.success("Notification delivery status",service.notifications(period,id)); }
    @PostMapping("/{id}/retry-notifications") public ApiResponse<?> retry(@PathVariable int period,@PathVariable long id) {
        return ApiResponse.success("Inbox delivery retried without reapplying amendment",service.retryNotifications(period,id));
    }
}

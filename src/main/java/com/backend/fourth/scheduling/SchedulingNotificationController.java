package com.backend.fourth.scheduling;

import com.backend.fourth.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/examination-notifications")
public class SchedulingNotificationController {
    private final SchedulingNotificationService service;
    @GetMapping public ApiResponse<?> inbox() { return ApiResponse.success("Your timetable notifications",service.inbox()); }
    @PostMapping("/{id}/read") public ApiResponse<?> read(@PathVariable long id) { service.read(id);return ApiResponse.success("Notification read",null); }
}

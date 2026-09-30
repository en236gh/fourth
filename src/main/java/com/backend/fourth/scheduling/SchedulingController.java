package com.backend.fourth.scheduling;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/examination-periods")
@PreAuthorize("hasAuthority('ADMINISTRATOR')")
@RequiredArgsConstructor
public class SchedulingController {
    private final SchedulingService service;
    private final AdminInvigilatorAssignmentService assignments;
    private final CurrentStaffResolver staff;

    @GetMapping
    public ApiResponse<?> list() { return ApiResponse.success("Examination periods",service.list()); }
    @GetMapping("/defaults")
    public ApiResponse<?> defaults() { return ApiResponse.success("Scheduling defaults",service.defaults()); }
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody SchedulingRequests.Period request) { return ApiResponse.success("Draft period created",service.create(request)); }
    @DeleteMapping("/{id}")
    public ApiResponse<?> deleteDraft(@PathVariable int id,@RequestParam long revision) { service.deleteDraftPeriod(id,revision); return ApiResponse.success("Draft period deleted",null); }
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable int id,@RequestParam long revision,@Valid @RequestBody SchedulingRequests.Period request) { return ApiResponse.success("Period setup updated",service.update(id,revision,request)); }
    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable int id) { return ApiResponse.success("Examination period",service.detail(id)); }
    @GetMapping("/{id}/courses")
    public ApiResponse<?> courses(@PathVariable int id,@RequestParam(required=false) Integer schoolId) { return ApiResponse.success("Eligible course counts",service.catalog(id,schoolId)); }
    @PutMapping("/{id}/courses")
    public ApiResponse<?> select(@PathVariable int id,@Valid @RequestBody SchedulingRequests.Selection request) { return ApiResponse.success("Course selection saved",service.select(id,request)); }
    @PostMapping("/{id}/generate")
    public ApiResponse<?> generate(@PathVariable int id,@Valid @RequestBody SchedulingRequests.Generate request) {
        var generation=service.generate(id,request);
        return new ApiResponse<>(generation.result().outcome()==ConstraintScheduler.Outcome.COMPLETE,generation.result().outcome().name(),generation);
    }
    @PostMapping("/{id}/reset-draft")
    public ApiResponse<?> reset(@PathVariable int id,@Valid @RequestBody SchedulingRequests.Revision request) { return ApiResponse.success("Unpublished draft cleared",service.reset(id,request.revision())); }
    @PutMapping("/{id}/exams/{session}/placement")
    public ApiResponse<?> edit(@PathVariable int id,@PathVariable int session,@Valid @RequestBody SchedulingRequests.Edit request) { return ApiResponse.success("Placement and allocations updated",service.edit(id,session,request)); }
    @GetMapping("/{id}/validation")
    public ApiResponse<?> validate(@PathVariable int id) { return ApiResponse.success("Publication validation",service.validate(id)); }
    @PostMapping("/{id}/publish")
    public ApiResponse<?> publish(@PathVariable int id,@Valid @RequestBody SchedulingRequests.Revision request) { return ApiResponse.success("Timetable and assignments published",service.publish(id,request.revision())); }
    @PostMapping("/{id}/auto-assign-invigilators")
    public ApiResponse<?> autoAssign(@PathVariable int id) { return ApiResponse.success("Draft staffing generated; review shortages",assignments.autoAssignPeriod(id,staff.requireCurrentStaff())); }
    @PostMapping("/{id}/invigilators")
    public ApiResponse<?> assign(@PathVariable int id,@Valid @RequestBody SchedulingRequests.Assignment request) { return ApiResponse.success("Draft assignment created",assignments.createForPeriod(id,request,staff.requireCurrentStaff())); }
    @GetMapping("/{id}/history")
    public ApiResponse<?> history(@PathVariable int id) { return ApiResponse.success("Timetable audit history",service.history(id)); }
    @GetMapping("/{id}/exams/{session}/alternatives")
    public ApiResponse<?> alternatives(@PathVariable int id,@PathVariable int session) { return ApiResponse.success("Suggested placements; save revalidates current data",service.alternatives(id,session)); }
    @GetMapping("/venues")
    public ApiResponse<?> venues() { return ApiResponse.success("Venues and examination capacities",service.venues()); }
    @PutMapping("/venues/{venue}/capacity")
    public ApiResponse<?> capacity(@PathVariable int venue,@Valid @RequestBody SchedulingRequests.Capacity request) { service.capacity(venue,request.examinationCapacity()); return ApiResponse.success("Examination capacity saved",null); }
    @GetMapping("/venue-unavailability")
    public ApiResponse<?> unavailability() { return ApiResponse.success("Venue unavailability",service.unavailability()); }
    @PostMapping("/venues/{venue}/unavailability")
    public ApiResponse<?> unavailable(@PathVariable int venue,@Valid @RequestBody SchedulingRequests.Unavailability request) { service.unavailable(venue,request); return ApiResponse.success("Venue unavailability recorded",null); }
    @GetMapping("/allocation-audit")
    public ApiResponse<?> audit() { return ApiResponse.success("Historical allocations needing review",service.audit()); }
}

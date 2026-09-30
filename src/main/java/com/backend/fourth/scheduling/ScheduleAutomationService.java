package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** One administrator action creates the timetable, allocations and staffing for review. */
@Service
@RequiredArgsConstructor
public class ScheduleAutomationService {
    private final SchedulingService scheduling;
    private final AdminInvigilatorAssignmentService assignments;
    private final CurrentStaffResolver staff;

    public record Review(SchedulingService.Generation generation, List<AutoAssignmentResponse> staffing,
                         SchedulingService.Validation validation) {}

    @Transactional
    public Review create(SchedulingRequests.Period request) {
        var period=scheduling.create(request);
        return generate(((Number)period.get("period_id")).intValue(),new SchedulingRequests.Generate(0,100000));
    }

    @Transactional
    public Review generate(int id,SchedulingRequests.Generate request) {
        var administrator=staff.requireCurrentStaff();
        var generation=scheduling.generate(id,request);
        if (generation.result().outcome()!=ConstraintScheduler.Outcome.COMPLETE)
            return new Review(generation,List.of(),new SchedulingService.Validation(false,generation.result().problems()));
        var staffing=assignments.autoAssignPeriod(id,administrator);
        return new Review(new SchedulingService.Generation(generation.result(),scheduling.detail(id)),staffing,scheduling.validate(id));
    }
}

package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.dto.AutoAssignmentResponse;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import com.backend.fourth.staff.entity.Staff;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScheduleAutomationServiceTest {
    private final SchedulingService scheduling=mock(SchedulingService.class);
    private final AdminInvigilatorAssignmentService assignments=mock(AdminInvigilatorAssignmentService.class);
    private final CurrentStaffResolver staff=mock(CurrentStaffResolver.class);
    private final ScheduleAutomationService automation=new ScheduleAutomationService(scheduling,assignments,staff);

    @Test void oneActionCreatesGeneratesAndStaffsThenReturnsFreshReview() {
        var actor=new Staff();
        when(staff.requireCurrentStaff()).thenReturn(actor);
        when(scheduling.create(null)).thenReturn(Map.of("period_id",12));
        var result=new ConstraintScheduler.Result(ConstraintScheduler.Outcome.COMPLETE,List.of(),List.of(),1,List.of());
        when(scheduling.generate(12,new SchedulingRequests.Generate(0,100000)))
                .thenReturn(new SchedulingService.Generation(result,Map.of("revision",1)));
        var shortages=List.of(new AutoAssignmentResponse(1,0,List.of(2),List.of()));
        when(assignments.autoAssignPeriod(12,actor)).thenReturn(shortages);
        when(scheduling.detail(12)).thenReturn(Map.of("revision",1,"assignments",List.of()));
        when(scheduling.validate(12)).thenReturn(new SchedulingService.Validation(false,List.of("Insufficient invigilators")));
        var review=automation.create(null);
        assertEquals(shortages,review.staffing());
        assertFalse(review.validation().valid());
        assertEquals(1,review.generation().period().get("revision"));
        var order=inOrder(scheduling,assignments);
        order.verify(scheduling).create(null);
        order.verify(scheduling).generate(12,new SchedulingRequests.Generate(0,100000));
        order.verify(assignments).autoAssignPeriod(12,actor);
        order.verify(scheduling).detail(12);
        order.verify(scheduling).validate(12);
        verify(scheduling,never()).publish(anyInt(),anyLong());
    }

    @Test void failedGenerationReturnsProblemsWithoutStaffingOrPublishing() {
        var request=new SchedulingRequests.Generate(3,1);
        var result=new ConstraintScheduler.Result(ConstraintScheduler.Outcome.SEARCH_LIMIT_REACHED,List.of(),List.of("A"),1,List.of("Search limit reached"));
        when(scheduling.generate(12,request)).thenReturn(new SchedulingService.Generation(result,Map.of("revision",3)));
        var review=automation.generate(12,request);
        assertEquals(result.problems(),review.validation().problems());
        assertFalse(review.validation().valid());
        verifyNoInteractions(assignments);
        verify(scheduling,never()).publish(anyInt(),anyLong());
    }
}

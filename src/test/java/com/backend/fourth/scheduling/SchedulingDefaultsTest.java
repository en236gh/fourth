package com.backend.fourth.scheduling;

import com.backend.fourth.invigilator.repository.AssignmentWriteLock;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchedulingDefaultsTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final SchedulingService service=spy(new SchedulingService(jdbc,mock(AssignmentWriteLock.class),mock(SchedulingAccess.class)));
    private SchedulingRequests.Period request(String year) {
        return new SchedulingRequests.Period("Finals",year,1,"FINAL",LocalDate.of(2090,1,2),LocalDate.of(2090,1,8),
                List.of(1,2,3,4,5),List.of(new SchedulingRequests.DailySlot(LocalTime.of(9,0),LocalTime.of(11,0))));
    }
    @Test void academicYearCanBeOmitted() {
        try(var factory=Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request(null)).isEmpty());
        }
    }
    @Test void defaultsReadRegistrationYearAndRejectMissingSource() {
        when(jdbc.queryForObject("SELECT max(academic_year) FROM student_registration",String.class)).thenReturn("2090/2091",null,"invalid");
        assertEquals("2090/2091",service.defaults().get("academicYear"));
        assertThrows(IllegalStateException.class,service::defaults);
        assertThrows(IllegalStateException.class,service::defaults);
    }
    @Test void createIgnoresClientYearAndUsesRegistrationYear() {
        when(jdbc.queryForObject("SELECT max(academic_year) FROM student_registration",String.class)).thenReturn("2090/2091");
        when(jdbc.queryForObject(contains("INSERT INTO examination_period("),eq(Integer.class),any(),any(),any(),any(),any(),any(),any())).thenReturn(12);
        doReturn(Map.of("period_id",12)).when(service).detail(12);
        service.create(request("1900/1901"));
        verify(jdbc).queryForObject(contains("INSERT INTO examination_period("),eq(Integer.class),eq("Finals"),eq("2090/2091"),eq(1),eq("FINAL"),eq(LocalDate.of(2090,1,2)),eq(LocalDate.of(2090,1,8)),eq("Africa/Lusaka"));
    }
    @Test void editingPreservesPeriodYearAfterRegistrationRollover() {
        when(jdbc.queryForList("SELECT * FROM examination_period WHERE period_id=?",12)).thenReturn(List.of(Map.of(
                "academic_year","2089/2090","timezone","Africa/Lusaka","status","DRAFT","revision",0L)));
        doReturn(Map.of("period_id",12)).when(service).detail(12);
        service.update(12,0,request("2091/2092"));
        verify(jdbc).update(startsWith("UPDATE examination_period SET name="),eq("Finals"),eq("2089/2090"),eq(1),eq("FINAL"),eq(LocalDate.of(2090,1,2)),eq(LocalDate.of(2090,1,8)),eq(12));
        verify(jdbc,never()).queryForObject("SELECT max(academic_year) FROM student_registration",String.class);
    }
}

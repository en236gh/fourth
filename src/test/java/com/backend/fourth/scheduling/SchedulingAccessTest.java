package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulingAccessTest {
    @Test void administratorIsNotAutomaticallyCoordinatorOrLead() {
        var jdbc=mock(JdbcTemplate.class); var resolver=mock(CurrentStaffResolver.class); var audit=mock(SchedulingDeniedAudit.class);
        var staff=new Staff();staff.setStaffId(7);staff.setAccountStatus("ACTIVE");var role=new Role();role.setName("ADMINISTRATOR");staff.setRoles(Set.of(role));
        when(resolver.requireCurrentStaff()).thenReturn(staff);
        var access=new SchedulingAccess(jdbc,resolver,audit);
        assertThrows(AccessDeniedException.class,()->access.coordinator(1));
        assertThrows(AccessDeniedException.class,access::lead);
        verify(audit).record(7,1);verify(audit).record(7,null);
        when(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND coordinator_staff_id=?)",Boolean.class,1,7)).thenReturn(true);
        assertDoesNotThrow(()->access.coordinator(1));
    }
}

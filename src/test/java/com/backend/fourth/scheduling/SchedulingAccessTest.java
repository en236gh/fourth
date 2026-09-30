package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulingAccessTest {
    @Test void activeAdministratorCanManageWithoutCoordinatorOrLeadRecords() {
        var jdbc=mock(JdbcTemplate.class); var resolver=mock(CurrentStaffResolver.class); var audit=mock(SchedulingDeniedAudit.class);
        var staff=new Staff();staff.setStaffId(7);staff.setAccountStatus("ACTIVE");var role=new Role();role.setName("ADMINISTRATOR");staff.setRoles(Set.of(role));
        when(resolver.requireCurrentStaff()).thenReturn(staff);
        var access=new SchedulingAccess(jdbc,resolver,audit);
        when(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND status='DRAFT')",Boolean.class,1)).thenReturn(true);
        assertTrue(access.canEdit(1));
        verifyNoInteractions(audit);
        verify(jdbc).queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND status='DRAFT')",Boolean.class,1);
    }
}

package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Called under the scheduling write lock, in the mutation's transaction. */
@Service
@RequiredArgsConstructor
public class SchedulingAccess {
    private final JdbcTemplate jdbc;
    private final CurrentStaffResolver staff;
    private final SchedulingDeniedAudit denied;

    public int administrator() {
        var actor = staff.requireCurrentStaff();
        if (!"ACTIVE".equals(actor.getAccountStatus()) || actor.getRoles().stream().noneMatch(r -> "ADMINISTRATOR".equals(r.getName()))) {
            denied.record(actor.getStaffId(), null);
            throw new AccessDeniedException("An active administrator account is required.");
        }
        jdbc.queryForObject("SELECT set_config('app.scheduling_actor',?,true)", String.class, actor.getStaffId().toString());
        return actor.getStaffId();
    }

    public boolean canEdit(int period) {
        var actor = staff.requireCurrentStaff();
        return "ACTIVE".equals(actor.getAccountStatus())
                && actor.getRoles().stream().anyMatch(r -> "ADMINISTRATOR".equals(r.getName()))
                && Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND status='DRAFT')", Boolean.class, period));
    }
}

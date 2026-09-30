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

    public void coordinator(int period) {
        int actor = administrator();
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND coordinator_staff_id=?)", Boolean.class, period, actor))) {
            denied.record(actor, period);
            throw new AccessDeniedException("Only the assigned timetable coordinator can change this period. Ask the coordinator to review your proposed change.");
        }
    }

    public void lead() {
        int actor = administrator();
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM scheduling_lead_permission WHERE staff_id=?)", Boolean.class, actor))) {
            denied.record(actor, null);
            throw new AccessDeniedException("Explicit scheduling lead permission is required to assign a coordinator.");
        }
    }

    public boolean canEdit(int period) {
        int actor = staff.requireCurrentStaff().getStaffId();
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM examination_period WHERE period_id=? AND coordinator_staff_id=?)", Boolean.class, period, actor));
    }

    public boolean isLead() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM scheduling_lead_permission WHERE staff_id=?)", Boolean.class, staff.requireCurrentStaff().getStaffId()));
    }
}

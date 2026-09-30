package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.*;

/** In-application inbox delivery. No SMTP or external messages are sent. */
@Service
@RequiredArgsConstructor
public class SchedulingNotificationService {
    private final JdbcTemplate jdbc;
    private final CurrentStaffResolver staff;
    public record Applied(long amendmentId) {}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void deliver(long amendment) {
        jdbc.update("""
                UPDATE examination_notification SET delivery_status='AVAILABLE',attempts=attempts+1,last_error=NULL,available_at=CURRENT_TIMESTAMP
                WHERE amendment_id=? AND delivery_status IN ('PENDING','FAILED')
                AND EXISTS(SELECT 1 FROM examination_amendment WHERE amendment_id=? AND status='APPLIED')
                """,amendment,amendment);
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void failed(long amendment) {
        jdbc.update("UPDATE examination_notification SET delivery_status='FAILED',attempts=attempts+1,last_error='Inbox delivery failed; retry delivery separately' WHERE amendment_id=? AND delivery_status IN ('PENDING','FAILED')",amendment);
    }
    private String[] recipient() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getAuthorities().stream().anyMatch(a->"STUDENT".equals(a.getAuthority()))) return new String[]{"STUDENT",auth.getName()};
        return new String[]{"STAFF",staff.requireCurrentStaff().getStaffId().toString()};
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> inbox() {
        var recipient=recipient();
        return jdbc.queryForList("SELECT notification_id,message,available_at,read_at FROM examination_notification WHERE recipient_kind=? AND recipient_id=? AND delivery_status='AVAILABLE' ORDER BY notification_id DESC",recipient[0],recipient[1]);
    }
    @Transactional
    public void read(long id) {
        var recipient=recipient();
        if(jdbc.update("UPDATE examination_notification SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE notification_id=? AND recipient_kind=? AND recipient_id=? AND delivery_status='AVAILABLE'",id,recipient[0],recipient[1])!=1)
            throw new org.springframework.security.access.AccessDeniedException("Notification is not available to this account.");
    }
}

package com.backend.fourth.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchedulingDeniedAudit {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(int actor, Integer period) {
        jdbc.update("INSERT INTO scheduling_audit(actor_staff_id,period_id,action,entity) VALUES (?,?,'DENIED','period_mutation')", actor, period);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHttp(String subject,String operation) {
        jdbc.update("INSERT INTO scheduling_audit(actor_subject,action,entity) VALUES (?,'HTTP_MUTATION_DENIED',?)",subject,operation);
    }
}


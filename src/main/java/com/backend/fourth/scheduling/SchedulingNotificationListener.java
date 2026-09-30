package com.backend.fourth.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SchedulingNotificationListener {
    private final SchedulingNotificationService notifications;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(SchedulingNotificationListener.class);
    @TransactionalEventListener
    public void applied(SchedulingNotificationService.Applied event) {
        try { notifications.deliver(event.amendmentId()); }
        catch(RuntimeException ex) {
            try { notifications.failed(event.amendmentId()); } catch(RuntimeException unavailable) {
                log.warn("Amendment {} committed; notification status remains pending because the database is unavailable",event.amendmentId());
            }
        }
    }
}

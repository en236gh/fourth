package com.backend.fourth.exam.scheduler;

import com.backend.fourth.exam.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExamSessionCompletionScheduler {
    private final ExamService examService;

    /**
     * Every 30 seconds, complete any IN_PROGRESS exams past their end_time
     * and mark remaining allocated students ABSENT.
     */
    @Scheduled(fixedDelayString = "${exam.completion.poll-ms:30000}")
    public void completeExpiredExamSessions() {
        int completed = examService.completeExpiredSessions();
        if (completed > 0) {
            log.info("Auto-completed {} exam session(s) past end_time", completed);
        }
    }
}

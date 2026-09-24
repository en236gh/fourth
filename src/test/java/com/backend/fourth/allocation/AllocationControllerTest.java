package com.backend.fourth.allocation;

import com.backend.fourth.allocation.controller.AllocationController;
import com.backend.fourth.allocation.service.AllocationService;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AllocationControllerTest {
    @Test
    void allocationPostIsNoLongerAvailable() throws Exception {
        var service = mock(AllocationService.class);
        var exams = mock(ExamSessionRepository.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AllocationController(service, exams)).build();

        mvc.perform(post("/api/allocation/exam-session/22"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(service, exams);
    }
}

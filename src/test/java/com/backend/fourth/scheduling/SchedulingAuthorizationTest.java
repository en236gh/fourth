package com.backend.fourth.scheduling;

import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.invigilator.service.AdminInvigilatorAssignmentService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulingAuthorizationTest {
    @Configuration @EnableMethodSecurity static class Security {}
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void onlyAdministratorsCanReadOrMutatePeriodWorkflow() {
        try(var context=new AnnotationConfigApplicationContext()) {
            var service=mock(SchedulingService.class);
            context.register(Security.class);
            context.registerBean(SchedulingService.class,()->service);
            context.registerBean(AdminInvigilatorAssignmentService.class,()->mock(AdminInvigilatorAssignmentService.class));
            context.registerBean(CurrentStaffResolver.class,()->mock(CurrentStaffResolver.class));
            context.registerBean(ScheduleAutomationService.class,()->mock(ScheduleAutomationService.class));
            context.registerBean(SchedulingController.class);context.refresh();
            var controller=context.getBean(SchedulingController.class);
            for(String role:List.of("STUDENT","LECTURER","INVIGILATOR")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user","",List.of(new SimpleGrantedAuthority(role))));
                assertThrows(AccessDeniedException.class,controller::list);
                assertThrows(AccessDeniedException.class,controller::defaults);
                assertThrows(AccessDeniedException.class,()->controller.create(null));
                assertThrows(AccessDeniedException.class,()->controller.generate(1,new SchedulingRequests.Generate(0,100000)));
                assertThrows(AccessDeniedException.class,()->controller.publish(1,new SchedulingRequests.Revision(0)));
                assertThrows(AccessDeniedException.class,controller::audit);
            }
            verifyNoInteractions(service);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority("ADMINISTRATOR"))));
            assertTrue(controller.list().success());verify(service).list();
        }
    }
}

package com.backend.fourth.attendance;

import com.backend.fourth.attendance.controller.OfflineAttendanceController;
import com.backend.fourth.attendance.service.OfflineAttendanceService;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.staff.entity.Staff;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OfflineAttendanceAuthorizationTest {
    @Configuration @EnableMethodSecurity static class Security {}
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void onlyAuthenticatedInvigilatorsCanDownloadAndSync() {
        try(var context=new AnnotationConfigApplicationContext()) {
            var service=mock(OfflineAttendanceService.class);
            var resolver=mock(CurrentStaffResolver.class);
            context.register(Security.class);
            context.registerBean(OfflineAttendanceService.class,()->service);
            context.registerBean(CurrentStaffResolver.class,()->resolver);
            context.registerBean(OfflineAttendanceController.class);context.refresh();
            var controller=context.getBean(OfflineAttendanceController.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class,controller::download);
            assertThrows(AuthenticationCredentialsNotFoundException.class,()->controller.sync(null));
            for(String role:List.of("STUDENT","LECTURER","ADMINISTRATOR")) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user","",List.of(new SimpleGrantedAuthority(role))));
                assertThrows(AccessDeniedException.class,controller::download);
                assertThrows(AccessDeniedException.class,()->controller.sync(null));
            }
            verifyNoInteractions(service,resolver);
            var actor=new Staff();actor.setStaffId(1);when(resolver.requireCurrentStaff()).thenReturn(actor);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("invigilator","",List.of(new SimpleGrantedAuthority("INVIGILATOR"))));
            assertEquals("no-store",controller.download().getHeaders().getCacheControl());
            controller.sync(new OfflineAttendanceController.SyncRequest(List.of()));
            verify(service).download(actor);verify(service).sync(List.of(),actor);
        }
    }
}

package com.backend.fourth.staff.service;

import com.backend.fourth.staff.dto.CreateStaffRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StaffAdminServiceTest {

    @Test
    void createStaffRequestShouldCaptureAdministratorEnteredStaffFields() {
        CreateStaffRequest request = new CreateStaffRequest(
                "Jane Staff",
                "jane.staff@example.edu",
                "+123456789",
                "Academic",
                "LECTURER"
        );

        assertNotNull(request);
        assertEquals("Jane Staff", request.fullName());
        assertEquals("LECTURER", request.role());
    }
}

package com.backend.fourth.staff.entity;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaffRoleMappingTest {

    @Test
    void staffShouldExposeRoleNamesFromTheRoleEntity() {
        Staff staff = new Staff();
        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        staff.setRoles(Set.of(adminRole));

        assertEquals(Set.of("ADMIN"), staff.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));
    }
}

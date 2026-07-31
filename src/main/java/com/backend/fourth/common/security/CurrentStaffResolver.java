package com.backend.fourth.common.security;

import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentStaffResolver {
    private final StaffRepository staffRepository;

    public Staff requireCurrentStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated staff found");
        }
        return staffRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated staff not found"));
    }
}

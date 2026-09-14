package com.backend.fourth.staff.service;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.staff.dto.CreateStaffRequest;
import com.backend.fourth.staff.dto.CreateStaffResponse;
import com.backend.fourth.staff.entity.Role;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.staff.entity.StaffActivationToken;
import com.backend.fourth.staff.repository.RoleRepository;
import com.backend.fourth.staff.repository.StaffActivationTokenRepository;
import com.backend.fourth.staff.repository.StaffRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StaffAdminService {
    private static final int TOKEN_BYTES = 32;
    private static final String DEFAULT_ACCOUNT_STATUS = "PENDING";
    private static final int TOKEN_TTL_HOURS = 24;

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final StaffActivationTokenRepository staffActivationTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ApiResponse<CreateStaffResponse> createStaff(@Valid CreateStaffRequest request) {
        if (staffRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT)).isPresent()) {
            throw new IllegalArgumentException("Staff account already exists for this email");
        }

        Role role = roleRepository.findByNameIgnoreCase(request.role().trim())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + request.role()));

        Staff staff = new Staff();
        staff.setFullName(request.fullName().trim());
        staff.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        staff.setPhone(request.phone().trim());
        staff.setDepartment(request.department().trim());
        staff.setPasswordHash(null);
        staff.getRoles().add(role);
        staff.setAccountStatus(DEFAULT_ACCOUNT_STATUS);
        staff = staffRepository.save(staff);

        String rawToken = generateActivationToken();
        String tokenHash = passwordEncoder.encode(rawToken);
        StaffActivationToken activationToken = new StaffActivationToken();
        activationToken.setStaff(staff);
        activationToken.setTokenHash(tokenHash);
        activationToken.setCreatedAt(LocalDateTime.now());
        activationToken.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_TTL_HOURS));
        activationToken.setUsedAt(null);
        staffActivationTokenRepository.save(activationToken);

        CreateStaffResponse response = new CreateStaffResponse(
                staff.getStaffId(),
                staff.getFullName(),
                staff.getEmail(),
                staff.getPhone(),
                staff.getDepartment(),
                role.getName(),
                staff.getAccountStatus(),
                rawToken,
                activationToken.getExpiresAt()
        );

        return ApiResponse.success("Staff account created and activation token generated", response);
    }

    private String generateActivationToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

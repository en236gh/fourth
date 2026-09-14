package com.backend.fourth.staff.repository;

import com.backend.fourth.staff.entity.StaffActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffActivationTokenRepository extends JpaRepository<StaffActivationToken, Integer> {
    Optional<StaffActivationToken> findByStaff_StaffId(Integer staffId);
}

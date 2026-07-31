package com.backend.fourth.staff.repository;

import com.backend.fourth.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Integer> {
    Optional<Staff> findByEmail(String email);
    Optional<Staff> findByStaffNo(String staffNo);
}

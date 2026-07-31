package com.backend.fourth.student.repository;

import com.backend.fourth.student.entity.StudentRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRefreshTokenRepository extends JpaRepository<StudentRefreshToken, Long> {
    Optional<StudentRefreshToken> findByToken(String token);
}

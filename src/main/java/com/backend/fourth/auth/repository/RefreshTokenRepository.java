package com.backend.fourth.auth.repository;

import com.backend.fourth.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @EntityGraph(attributePaths = "staff")
    Optional<RefreshToken> findByToken(String token);
}

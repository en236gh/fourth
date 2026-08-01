package com.backend.fourth.student.service;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ExamPassQrService {
    public static final String PURPOSE_EXAM_PASS = "EXAM_PASS";

    private final JwtProperties jwtProperties;
    private final long validityHoursAfterEnd;

    public ExamPassQrService(
            JwtProperties jwtProperties,
            @Value("${app.exam-pass.qr-validity-hours-after-end:6}") long validityHoursAfterEnd) {
        this.jwtProperties = jwtProperties;
        this.validityHoursAfterEnd = validityHoursAfterEnd;
    }

    public SignedQrToken sign(
            String computerNumber,
            String academicYear,
            Integer semester,
            List<ExamSession> allocatedSessions) {
        if (allocatedSessions == null || allocatedSessions.isEmpty()) {
            throw new IllegalArgumentException("At least one allocated examination is required to sign a pass");
        }

        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = allocatedSessions.stream()
                .map(this::examEndInstant)
                .max(Comparator.naturalOrder())
                .orElseThrow()
                .plusSeconds(validityHoursAfterEnd * 3600);

        String token = Jwts.builder()
                .id(jti)
                .subject(computerNumber)
                .claim("purpose", PURPOSE_EXAM_PASS)
                .claim("academicYear", academicYear)
                .claim("semester", semester)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();

        return new SignedQrToken(token, jti, expiresAt);
    }

    public Claims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("QR token is required");
        }

        final Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            throw new IllegalArgumentException("Examination pass QR token has expired");
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid examination pass QR token");
        }

        if (!PURPOSE_EXAM_PASS.equals(String.valueOf(claims.get("purpose")))) {
            throw new IllegalArgumentException("QR token is not an examination pass token");
        }
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new IllegalArgumentException("Examination pass QR token has expired");
        }
        return claims;
    }

    private Instant examEndInstant(ExamSession examSession) {
        LocalDateTime end = LocalDateTime.of(examSession.getExamDate(), examSession.getEndTime());
        return end.atZone(ZoneId.systemDefault()).toInstant();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public record SignedQrToken(String token, String jti, Instant expiresAt) {
    }
}

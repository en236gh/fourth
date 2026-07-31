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
import java.util.Date;
import java.util.UUID;

@Service
public class ExamSlipQrService {
    public static final String PURPOSE_EXAM_SLIP = "EXAM_SLIP";

    private final JwtProperties jwtProperties;
    private final long validityHoursAfterEnd;

    public ExamSlipQrService(
            JwtProperties jwtProperties,
            @Value("${app.exam-slip.qr-validity-hours-after-end:6}") long validityHoursAfterEnd) {
        this.jwtProperties = jwtProperties;
        this.validityHoursAfterEnd = validityHoursAfterEnd;
    }

    public SignedQrToken sign(String computerNumber, ExamSession examSession) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = examEndInstant(examSession).plusSeconds(validityHoursAfterEnd * 3600);

        String token = Jwts.builder()
                .id(jti)
                .subject(computerNumber)
                .claim("purpose", PURPOSE_EXAM_SLIP)
                .claim("examSessionId", examSession.getExamSessionId())
                .claim("courseCode", examSession.getCourseCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();

        return new SignedQrToken(token, jti, expiresAt);
    }

    public Claims parseAndValidate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (!PURPOSE_EXAM_SLIP.equals(String.valueOf(claims.get("purpose")))) {
            throw new IllegalArgumentException("QR token is not an examination slip token");
        }
        if (claims.getExpiration() != null && claims.getExpiration().before(new Date())) {
            throw new IllegalArgumentException("Examination slip QR token has expired");
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

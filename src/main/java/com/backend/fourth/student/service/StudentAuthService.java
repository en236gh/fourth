package com.backend.fourth.student.service;

import com.backend.fourth.security.JwtService;
import com.backend.fourth.student.dto.ActivateAccountRequest;
import com.backend.fourth.student.dto.StudentLoginRequest;
import com.backend.fourth.student.dto.StudentLoginResponse;
import com.backend.fourth.student.dto.StudentProfileResponse;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.entity.StudentRefreshToken;
import com.backend.fourth.student.repository.StudentRefreshTokenRepository;
import com.backend.fourth.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAuthService {
    private static final List<String> STUDENT_ROLES = List.of("STUDENT");

    private final StudentRepository studentRepository;
    private final StudentRefreshTokenRepository studentRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public StudentProfileResponse activate(ActivateAccountRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        Student student = studentRepository
                .findByComputerNumberAndNationalId(request.computerNumber().trim(), request.nationalId().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No student found with the provided computer number and national ID"));

        if (student.isAccountActivated()) {
            throw new IllegalStateException("Account has already been activated");
        }

        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new IllegalStateException("Student account status does not allow activation: " + student.getStatus());
        }

        student.setPasswordHash(passwordEncoder.encode(request.password()));
        student.setAccountActivated(true);
        student.setActivatedAt(LocalDateTime.now());
        studentRepository.save(student);

        return toProfile(student);
    }

    @Transactional
    public StudentLoginResponse login(StudentLoginRequest request) {
        Student student = studentRepository.findByComputerNumber(request.computerNumber().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!student.isAccountActivated() || student.getPasswordHash() == null) {
            throw new IllegalArgumentException("Account has not been activated");
        }

        if (!passwordEncoder.matches(request.password(), student.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new IllegalStateException("Student account is not active: " + student.getStatus());
        }

        return issueTokens(student);
    }

    @Transactional
    public StudentLoginResponse refresh(String refreshTokenValue) {
        StudentRefreshToken stored = studentRefreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            studentRefreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        if (!jwtService.isTokenValid(refreshTokenValue)) {
            stored.setRevoked(true);
            studentRefreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Student student = stored.getStudent();
        String username = jwtService.extractUsername(refreshTokenValue);
        if (student == null || student.getComputerNumber() == null
                || !student.getComputerNumber().equals(username)) {
            stored.setRevoked(true);
            studentRefreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Invalid refresh token");
        }

        stored.setRevoked(true);
        studentRefreshTokenRepository.save(stored);

        return issueTokens(student);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(String computerNumber) {
        Student student = studentRepository.findByComputerNumber(computerNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return toProfile(student);
    }

    private StudentLoginResponse issueTokens(Student student) {
        String accessToken = jwtService.generateAccessToken(student.getComputerNumber(), STUDENT_ROLES);
        String refreshToken = jwtService.generateRefreshToken(student.getComputerNumber());

        StudentRefreshToken entity = new StudentRefreshToken();
        entity.setStudent(student);
        entity.setToken(refreshToken);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        entity.setRevoked(false);
        studentRefreshTokenRepository.save(entity);

        return new StudentLoginResponse(accessToken, refreshToken, toProfile(student));
    }

    private StudentProfileResponse toProfile(Student student) {
        return new StudentProfileResponse(
                student.getComputerNumber(),
                student.getFullName(),
                student.getSchool(),
                student.getProgram(),
                student.getYearOfStudy(),
                student.getStatus(),
                student.isAccountActivated()
        );
    }
}

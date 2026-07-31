package com.backend.fourth.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student")
@Getter
@Setter
public class Student {
    @Id
    @Column(name = "computer_number", nullable = false)
    private String computerNumber;

    @Column(name = "national_id", nullable = false, unique = true)
    private String nationalId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "program", nullable = false)
    private String program;

    @Column(name = "school", nullable = false)
    private String school;

    @Column(name = "year_of_study", nullable = false)
    private Integer yearOfStudy;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "photo_path", nullable = false)
    private String photoPath;

    @Column(name = "qr_token", nullable = false)
    private String qrToken;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "account_activated", nullable = false)
    private boolean accountActivated;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
}

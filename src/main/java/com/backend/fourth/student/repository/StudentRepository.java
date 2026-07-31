package com.backend.fourth.student.repository;

import com.backend.fourth.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByComputerNumber(String computerNumber);

    Optional<Student> findByComputerNumberAndNationalId(String computerNumber, String nationalId);
}

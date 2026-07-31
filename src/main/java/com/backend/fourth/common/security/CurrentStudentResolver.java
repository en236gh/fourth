package com.backend.fourth.common.security;

import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentStudentResolver {
    private final StudentRepository studentRepository;

    public Student requireCurrentStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated student found");
        }
        return studentRepository.findByComputerNumber(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated student not found"));
    }
}

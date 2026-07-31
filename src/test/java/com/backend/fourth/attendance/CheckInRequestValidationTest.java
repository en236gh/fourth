package com.backend.fourth.attendance;

import com.backend.fourth.attendance.dto.CheckInRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CheckInRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invalidComputerNumberIsRejected() {
        CheckInRequest request = new CheckInRequest("CS-2023-001", 1, 1, "COMPUTER");

        Set<ConstraintViolation<CheckInRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Computer number must be 10 digits and start with a four-digit year");
    }

    @Test
    void validComputerNumberIsAccepted() {
        CheckInRequest request = new CheckInRequest("2022004264", 1, 1, "COMPUTER");

        Set<ConstraintViolation<CheckInRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}

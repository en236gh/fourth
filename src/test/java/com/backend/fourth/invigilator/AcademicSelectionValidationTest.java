package com.backend.fourth.invigilator;

import com.backend.fourth.invigilator.dto.AcademicSelection;
import com.backend.fourth.invigilator.dto.CreateInvigilatorAssignmentRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicSelectionValidationTest {
    @Test
    void rejectsLegacyRequestWithoutSelection() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(
                    new CreateInvigilatorAssignmentRequest(null, 10, 1, 2, null));
            assertEquals(1, violations.size());
            assertEquals("selection", violations.iterator().next().getPropertyPath().toString());
        }
    }

    @Test
    void validatesNestedSelectionFields() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var invalid = new CreateInvigilatorAssignmentRequest(
                    new AcademicSelection(null, -1, 0, " "), 10, 1, 2, null);
            assertEquals(4, validator.validate(invalid).size());
            var valid = new CreateInvigilatorAssignmentRequest(
                    new AcademicSelection(1, 2, 3, "CSC3101"), 10, 1, 2, null);
            assertTrue(validator.validate(valid).isEmpty());
        }
    }
}

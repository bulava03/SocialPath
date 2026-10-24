package com.socialpath;

import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.validation.ValidationResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    @Test
    void success_HasNoMessages() {
        ValidationResult result = ValidationResult.success();

        assertTrue(result.isSuccess());
        assertTrue(result.getErrorMessages().isEmpty());
        assertEquals("", result.getMessage());
    }

    @Test
    void failure_WithSingleMessage_ExposesIt() {
        ValidationResult result = ValidationResult.failure("bad input");

        assertFalse(result.isSuccess());
        assertEquals("bad input", result.getMessage());
    }

    @Test
    void failure_WithSeveralMessages_ReturnsFirstAsMessage() {
        ValidationResult result = ValidationResult.failure(List.of("first", "second"));

        assertFalse(result.isSuccess());
        assertEquals("first", result.getMessage());
        assertEquals(2, result.getErrorMessages().size());
    }

    @Test
    void of_WithoutViolations_IsSuccess() {
        assertTrue(ValidationResult.of(Set.of()).isSuccess());
    }

    @Test
    void of_WithViolation_FailsWithItsMessage() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        GroupCreationForm form = new GroupCreationForm("alice");

        ValidationResult result = ValidationResult.of(validator.validate(form));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorMessages().size());
    }
}

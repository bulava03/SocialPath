package com.socialpath;

import com.socialpath.dto.request.GroupForm;
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
    void success_HasNoMessages_FailureShowsItsFirst() {
        ValidationResult success = ValidationResult.success();
        assertTrue(success.isSuccess());
        assertEquals("", success.getMessage());

        ValidationResult failure = ValidationResult.failure(List.of("first", "second"));
        assertFalse(failure.isSuccess());
        assertEquals("first", failure.getMessage());
        assertEquals(2, failure.getErrorMessages().size());
    }

    @Test
    void of_ConstraintViolations_FailsWithTheirMessage() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(ValidationResult.of(Set.of()).isSuccess());

        // A group form without a name breaks exactly one rule.
        ValidationResult result = ValidationResult.of(validator.validate(new GroupForm()));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorMessages().size());
    }
}

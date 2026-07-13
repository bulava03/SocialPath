package com.socialpath;

import com.socialpath.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

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
}

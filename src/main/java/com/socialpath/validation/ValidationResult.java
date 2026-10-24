package com.socialpath.validation;

import jakarta.validation.ConstraintViolation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ValidationResult {
    private final boolean success;
    private final List<String> errorMessages;

    private ValidationResult(boolean success, List<String> errorMessages) {
        this.success = success;
        this.errorMessages = errorMessages;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<String> errorMessages) {
        return new ValidationResult(false, errorMessages);
    }

    public static ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, Collections.singletonList(errorMessage));
    }

    /**
     * Turns Bean Validation output into a result.
     * @param violations the violations reported by the validator
     * @return success when there are none, otherwise a failure with their messages
     */
    public static ValidationResult of(Set<? extends ConstraintViolation<?>> violations) {
        if (violations.isEmpty()) {
            return success();
        }
        return failure(violations.stream().map(ConstraintViolation::getMessage).toList());
    }

    public boolean isSuccess() {
        return success;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public String getMessage() {
        return errorMessages.stream().findFirst().orElse("");
    }
}

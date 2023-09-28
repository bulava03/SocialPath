package com.example.SocialPath_Admin.other;

import java.util.Collections;
import java.util.List;

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

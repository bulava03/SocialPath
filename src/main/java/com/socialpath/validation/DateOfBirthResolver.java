package com.socialpath.validation;

import com.socialpath.dto.request.DateOfBirthInput;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * Assembles the optional date of birth from the day/month/year form fields.
 * All three empty means "not set"; a partially filled or impossible date is
 * rejected so the form can be shown again with the values the user typed.
 */
public final class DateOfBirthResolver {

    static final String INCOMPLETE = "Enter the full date of birth or leave all three fields empty.";
    static final String INVALID = "Invalid date entered";
    static final String NOT_PAST = "Date of birth must be in the past.";

    private static final int MIN_YEAR = 1900;

    private DateOfBirthResolver() {
    }

    public static ValidationResult resolve(DateOfBirthInput input) {
        return resolve(input, LocalDate.now());
    }

    /**
     * Validates the raw fields and, on success, stores the date (at midnight)
     * via {@link DateOfBirthInput#setDateOfBirth}, or null when no date was given.
     * @param input the submitted form
     * @param today the current date, passed in to keep the check testable
     * @return success, or a failure with a message for the form
     */
    static ValidationResult resolve(DateOfBirthInput input, LocalDate today) {
        String day = trim(input.getDay());
        String month = trim(input.getMonth());
        String year = trim(input.getYear());

        if (day.isEmpty() && month.isEmpty() && year.isEmpty()) {
            input.setDateOfBirth(null);
            return ValidationResult.success();
        }
        if (day.isEmpty() || month.isEmpty() || year.isEmpty()) {
            return ValidationResult.failure(INCOMPLETE);
        }
        LocalDate date = parseDate(day, month, year, today);
        if (date == null) {
            return ValidationResult.failure(INVALID);
        }
        if (date.isAfter(today)) {
            return ValidationResult.failure(NOT_PAST);
        }

        input.setDateOfBirth(date.atStartOfDay());
        return ValidationResult.success();
    }

    /**
     * Turns three non-empty fields into a calendar date.
     * @param day the day field
     * @param month the month field, 1-12
     * @param year the year field
     * @param today the current date, the upper bound for the year
     * @return the date, or null when a field is not a number, the year is out
     *         of range, or the date does not exist (e.g. 31 April)
     */
    private static LocalDate parseDate(String day, String month, String year, LocalDate today) {
        if (!isNumber(day) || !isNumber(month) || !isNumber(year)) {
            return null;
        }
        int yearValue = Integer.parseInt(year);
        if (yearValue < MIN_YEAR || yearValue > today.getYear()) {
            return null;
        }
        try {
            return LocalDate.of(yearValue, Integer.parseInt(month), Integer.parseInt(day));
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isNumber(String value) {
        return value.matches("\\d{1,4}");
    }
}

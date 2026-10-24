package com.socialpath.validation;

import com.socialpath.dto.request.RegistrationForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateOfBirthResolverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    private static RegistrationForm form(String day, String month, String year) {
        RegistrationForm form = new RegistrationForm();
        form.setDay(day);
        form.setMonth(month);
        form.setYear(year);
        return form;
    }

    @Test
    void resolve_WithValidDate_SetsDateAtMidnight() {
        RegistrationForm form = form(" 5 ", "3", "1999");

        ValidationResult result = DateOfBirthResolver.resolve(form, TODAY);

        assertTrue(result.isSuccess());
        assertEquals(LocalDateTime.of(1999, 3, 5, 0, 0), form.getDateOfBirth());
    }

    @Test
    void resolve_WithAllFieldsEmpty_ClearsDate() {
        RegistrationForm form = form("", null, null);
        form.setDateOfBirth(LocalDateTime.of(2000, 1, 1, 0, 0));

        ValidationResult result = DateOfBirthResolver.resolve(form, TODAY);

        assertTrue(result.isSuccess());
        assertNull(form.getDateOfBirth());
    }

    @Test
    void resolve_WithPartialDate_Fails() {
        ValidationResult result = DateOfBirthResolver.resolve(form("5", "3", null), TODAY);

        assertFalse(result.isSuccess());
        assertEquals(DateOfBirthResolver.INCOMPLETE, result.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "abc, 3, 1999",
            "-1, 3, 1999",
            "31, 4, 1999",
            "29, 2, 2023",
            "1, 13, 1999",
            "1, 1, 1899",
            "1, 1, 2027"
    })
    void resolve_WithImpossibleDate_Fails(String day, String month, String year) {
        ValidationResult result = DateOfBirthResolver.resolve(form(day, month, year), TODAY);

        assertFalse(result.isSuccess());
        assertEquals(DateOfBirthResolver.INVALID, result.getMessage());
    }

    @Test
    void resolve_WithLeapDay_Succeeds() {
        assertTrue(DateOfBirthResolver.resolve(form("29", "2", "2024"), TODAY).isSuccess());
    }

    @Test
    void resolve_WithDateLaterThisYear_Fails() {
        ValidationResult result = DateOfBirthResolver.resolve(form("25", "10", "2026"), TODAY);

        assertFalse(result.isSuccess());
        assertEquals(DateOfBirthResolver.NOT_PAST, result.getMessage());
    }
}

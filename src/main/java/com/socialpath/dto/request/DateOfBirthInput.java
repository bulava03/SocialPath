package com.socialpath.dto.request;

import java.time.LocalDateTime;

/**
 * A form that collects the date of birth as three raw text fields
 * (day, month, year) and holds the assembled date once it has been checked.
 * Implemented by the registration and profile-edit forms so both share
 * {@link com.socialpath.validation.DateOfBirthResolver}.
 */
public interface DateOfBirthInput {
    String getDay();

    String getMonth();

    String getYear();

    void setDateOfBirth(LocalDateTime dateOfBirth);
}

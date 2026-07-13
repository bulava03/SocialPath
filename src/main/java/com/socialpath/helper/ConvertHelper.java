package com.socialpath.helper;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

public class ConvertHelper {

    /**
     * Converts a month number to its English display name, matching the
     * option labels used by the registration and profile forms.
     * @param month month number, 1-12
     * @return full English month name, e.g. "January"
     */
    public static String monthToString(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month value: " + month);
        }
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}

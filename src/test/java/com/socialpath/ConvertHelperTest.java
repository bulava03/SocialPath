package com.socialpath;

import com.socialpath.helper.ConvertHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConvertHelperTest {

    @Test
    void monthToString_WithValidInput_ReturnsCorrectMonth() {
        // Assert
        assertEquals("January", ConvertHelper.monthToString(1));
        assertEquals("February", ConvertHelper.monthToString(2));
        assertEquals("March", ConvertHelper.monthToString(3));
        assertEquals("April", ConvertHelper.monthToString(4));
        assertEquals("May", ConvertHelper.monthToString(5));
        assertEquals("June", ConvertHelper.monthToString(6));
        assertEquals("July", ConvertHelper.monthToString(7));
        assertEquals("August", ConvertHelper.monthToString(8));
        assertEquals("September", ConvertHelper.monthToString(9));
        assertEquals("October", ConvertHelper.monthToString(10));
        assertEquals("November", ConvertHelper.monthToString(11));
        assertEquals("December", ConvertHelper.monthToString(12));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1, 100})
    void monthToString_WithInvalidInput_ThrowsException(int month) {
        // Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ConvertHelper.monthToString(month);
        });

        assertEquals("Invalid month value: " + month, exception.getMessage());
    }
}

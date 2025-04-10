package com.example.SocialPath;

import com.example.SocialPath.helper.ConvertHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConvertHelperTest {

    @Test
    void monthToString_WithValidInput_ReturnsCorrectMonth() {
        // Assert
        assertEquals("Січень", ConvertHelper.monthToString(1));
        assertEquals("Лютий", ConvertHelper.monthToString(2));
        assertEquals("Березень", ConvertHelper.monthToString(3));
        assertEquals("Квітень", ConvertHelper.monthToString(4));
        assertEquals("Травень", ConvertHelper.monthToString(5));
        assertEquals("Червень", ConvertHelper.monthToString(6));
        assertEquals("Липень", ConvertHelper.monthToString(7));
        assertEquals("Серпень", ConvertHelper.monthToString(8));
        assertEquals("Вересень", ConvertHelper.monthToString(9));
        assertEquals("Жовтень", ConvertHelper.monthToString(10));
        assertEquals("Листопад", ConvertHelper.monthToString(11));
        assertEquals("Грудень", ConvertHelper.monthToString(12));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 13, -1, 100})
    void monthToString_WithInvalidInput_ThrowsException(int month) {
        // Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ConvertHelper.monthToString(month);
        });

        assertEquals("Невірне значення місяця.", exception.getMessage());
    }
}

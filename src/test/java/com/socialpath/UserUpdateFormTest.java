package com.socialpath;

import com.socialpath.dto.request.UserUpdate;
import com.socialpath.entity.Country;
import com.socialpath.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The profile form shown for editing is filled from the stored user. */
class UserUpdateFormTest {

    private static User anna() {
        User user = new User();
        user.setLogin("anna");
        user.setFirstName("Anna");
        user.setLastName("Example");
        user.setEmail("anna@example.com");
        user.setPhone("+12015550123");
        user.setCity("Northtown");
        user.setWorkplace("Example Ltd");
        return user;
    }

    @Test
    void from_CopiesTheProfileAndSplitsTheDateOfBirth() {
        User user = anna();
        user.setDateOfBirth(LocalDateTime.of(1990, 3, 7, 0, 0));
        Country country = new Country();
        country.setCode("XX");
        user.setCountry(country);

        UserUpdate form = UserUpdate.from(user);

        assertEquals("anna", form.getLogin());
        assertEquals("Anna", form.getFirstName());
        assertEquals("Example", form.getLastName());
        assertEquals("anna@example.com", form.getEmail());
        // Back in the two fields of the form: the country code and the local number.
        assertEquals("US", form.getPhoneRegion());
        assertEquals("201-555-0123", form.getPhoneNumber());
        assertEquals("7", form.getDay());
        assertEquals("3", form.getMonth());
        assertEquals("1990", form.getYear());
        assertEquals("XX", form.getCountryCode());
        assertEquals("Northtown", form.getCity());
        assertEquals("Example Ltd", form.getWorkplace());
    }

    @Test
    void from_WithoutDateOfBirthOrCountry_LeavesThemEmpty() {
        UserUpdate form = UserUpdate.from(anna());

        assertNull(form.getDay());
        assertNull(form.getMonth());
        assertNull(form.getYear());
        assertNull(form.getCountryCode());
    }

    @Test
    void from_AnOldUnreadableNumber_IsOfferedAsTypedWithTheProfileCountry() {
        User user = anna();
        user.setPhone(null);
        user.setPhoneLegacy("555-0100");
        Country country = new Country();
        country.setCode("US");
        user.setCountry(country);

        UserUpdate form = UserUpdate.from(user);

        assertEquals("US", form.getPhoneRegion());
        assertEquals("555-0100", form.getPhoneNumber());
    }
}

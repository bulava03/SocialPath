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
        user.setPhoneNumber("555-0100");
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
        assertEquals("555-0100", form.getPhoneNumber());
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
}

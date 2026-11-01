package com.socialpath;

import com.socialpath.entity.Country;
import com.socialpath.entity.PhoneVisibility;
import com.socialpath.entity.User;
import com.socialpath.validation.PhoneNumbers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNumbersTest {

    @ParameterizedTest(name = "{0} / {1} -> {2}")
    @CsvSource(delimiter = '|', nullValues = "none", value = {
        // Written any common way, a local number of the chosen country.
        "US   | 201 555 0123     | +12015550123",
        "US   | (201) 555-0123   | +12015550123",
        "GB   | 020 7946 0018    | +442079460018",
        // The trunk 0 may be left out, as it is after the country code.
        "GB   | 20 7946 0018     | +442079460018",
        // With + and its own code the chosen country does not matter.
        "GB   | +1 201-555-0123  | +12015550123",
        "none | +44 20 7946 0018 | +442079460018",
        // Not a real number, or no way to tell the country.
        "US   | 555-0100         | none",
        "US   | 123              | none",
        "none | 201 555 0123     | none",
        "US   | not a number     | none",
    })
    void toE164(String region, String typed, String expected) {
        assertEquals(Optional.ofNullable(expected), PhoneNumbers.toE164(region, typed));
    }

    @Test
    void storedNumber_IsShownGroupedAndBackInItsCountry() {
        assertEquals("+1 201-555-0123", PhoneNumbers.formatInternational("+12015550123"));
        assertEquals("201-555-0123", PhoneNumbers.formatWithoutCountryCode("+12015550123"));
        // No trunk 0 next to the country code list.
        assertEquals("20 7946 0018", PhoneNumbers.formatWithoutCountryCode("+442079460018"));
        assertEquals("US", PhoneNumbers.regionOf("+12015550123"));
    }

    @Test
    void codeList_HasOnlyCountriesWithAPhoneCode_ByName() {
        List<PhoneNumbers.Region> regions = PhoneNumbers.regions(List.of(
                country("US", "United States"), country("ZZ", "Nowhere"), country("GB", "United Kingdom")));

        assertEquals(List.of(new PhoneNumbers.Region("GB", "United Kingdom (+44)"),
                new PhoneNumbers.Region("US", "United States (+1)")), regions);
    }

    @Test
    void phone_IsSeenByTheOwnerAndWhomTheVisibilityAllows() {
        User owner = new User();
        owner.setLogin("owner");
        owner.getFriends().add("friend");

        assertVisible(owner, PhoneVisibility.EVERYONE, true, true);
        assertVisible(owner, PhoneVisibility.FRIENDS, true, false);
        assertVisible(owner, PhoneVisibility.NOBODY, false, false);
    }

    private static void assertVisible(User owner, PhoneVisibility visibility, boolean friend, boolean stranger) {
        owner.setPhoneVisibility(visibility);
        assertTrue(owner.phoneVisibleTo("owner"), visibility + ": the owner");
        assertEquals(friend, owner.phoneVisibleTo("friend"), visibility + ": a friend");
        assertEquals(stranger, owner.phoneVisibleTo("stranger"), visibility + ": a stranger");
    }

    private static Country country(String code, String name) {
        Country country = new Country();
        country.setCode(code);
        country.setName(name);
        return country;
    }
}

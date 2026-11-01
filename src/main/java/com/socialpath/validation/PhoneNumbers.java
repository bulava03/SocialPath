package com.socialpath.validation;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.socialpath.entity.Country;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Phone numbers with Google's libphonenumber: a number is accepted only when
 * it exists for its country, is stored in the international E.164 form
 * (+12015550123) and is shown grouped the way people write it
 * (+1 201-555-0123).
 */
public final class PhoneNumbers {

    static final String REQUIRED = "Phone number is required.";
    static final String INVALID = "Enter a phone number that exists in the chosen country, or start it with + and the country code.";

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();
    /** The region libphonenumber uses when none is known: then only "+..." numbers parse. */
    private static final String NO_REGION = "ZZ";

    /**
     * A country in the phone code list.
     * @param code the ISO country code, e.g. "US"
     * @param label what the list shows, e.g. "United States (+1)"
     */
    public record Region(String code, String label) {
    }

    private PhoneNumbers() {
    }

    /**
     * Reads a number as typed.
     * @param region the ISO code of the chosen country; ignored when the
     *        number starts with + and its own country code
     * @param input the number as typed, with any spaces, dashes or brackets
     * @return the number in E.164, or empty when it is not a real number
     */
    public static Optional<String> toE164(String region, String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        try {
            String knownRegion = region == null || region.isBlank() ? NO_REGION : region;
            PhoneNumber number = UTIL.parse(input, knownRegion);
            return UTIL.isValidNumber(number)
                    ? Optional.of(UTIL.format(number, PhoneNumberFormat.E164))
                    : Optional.empty();
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Checks the phone fields of a form and, on success, stores the number in
     * E.164 through {@link PhoneInput#setPhone}.
     * @param input the submitted form
     * @return success, or a failure with a message for the form
     */
    public static ValidationResult resolve(PhoneInput input) {
        if (input.getPhoneNumber() == null || input.getPhoneNumber().isBlank()) {
            return ValidationResult.failure(REQUIRED);
        }
        Optional<String> e164 = toE164(input.getPhoneRegion(), input.getPhoneNumber());
        if (e164.isEmpty()) {
            return ValidationResult.failure(INVALID);
        }
        input.setPhone(e164.get());
        return ValidationResult.success();
    }

    /**
     * The number grouped for reading, with its country code.
     * @param e164 a stored number
     * @return e.g. "+1 201-555-0123"
     */
    public static String formatInternational(String e164) {
        return format(e164, PhoneNumberFormat.INTERNATIONAL);
    }

    /**
     * The number for the edit form, where the country code list stands
     * beside it: grouped, without the country code and without the trunk
     * prefix many countries dial at home (the 0 of 020 7946 0018), which
     * does not belong after a country code.
     * @param e164 a stored number
     * @return e.g. "201-555-0123" or "20 7946 0018"
     */
    public static String formatWithoutCountryCode(String e164) {
        String international = formatInternational(e164);
        String countryCode = "+" + parseStored(e164).getCountryCode() + " ";
        return international.startsWith(countryCode) ? international.substring(countryCode.length()) : international;
    }

    /**
     * The country a stored number belongs to.
     * @param e164 a stored number
     * @return its ISO code, e.g. "US"
     */
    public static String regionOf(String e164) {
        return UTIL.getRegionCodeForNumber(parseStored(e164));
    }

    /**
     * The country code list for the forms: the countries that have a phone
     * code, by name.
     * @param countries the reference countries
     * @return the list entries
     */
    public static List<Region> regions(List<Country> countries) {
        return countries.stream()
                .filter(country -> UTIL.getCountryCodeForRegion(country.getCode()) != 0)
                .sorted(Comparator.comparing(Country::getName))
                .map(country -> new Region(country.getCode(),
                        country.getName() + " (+" + UTIL.getCountryCodeForRegion(country.getCode()) + ")"))
                .toList();
    }

    private static String format(String e164, PhoneNumberFormat format) {
        return UTIL.format(parseStored(e164), format);
    }

    private static PhoneNumber parseStored(String e164) {
        try {
            return UTIL.parse(e164, NO_REGION);
        } catch (NumberParseException e) {
            throw new IllegalArgumentException("Not a stored phone number: " + e164, e);
        }
    }
}

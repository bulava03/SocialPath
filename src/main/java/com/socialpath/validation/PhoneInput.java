package com.socialpath.validation;

/**
 * A form that asks for a phone number: a country from the code list and the
 * number as typed. Implemented by the registration and profile forms so both
 * share {@link PhoneNumbers#resolve}.
 */
public interface PhoneInput {
    String getPhoneRegion();

    String getPhoneNumber();

    /**
     * Receives the number once it has been checked.
     * @param e164 the number in E.164, e.g. "+12015550123"
     */
    void setPhone(String e164);

    /**
     * The checked number.
     * @return the number in E.164, or null before it is checked
     */
    String getPhone();
}

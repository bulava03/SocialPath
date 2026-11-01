package com.socialpath.dto.request;

import com.socialpath.entity.ContactVisibility;
import com.socialpath.entity.User;
import com.socialpath.validation.PhoneInput;
import com.socialpath.validation.PhoneNumbers;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * The profile-edit form. It also backs the form when it is shown: filled from
 * the stored user at first, and with the values as typed when an update is
 * rejected, so the user sees exactly what they entered.
 */
@Data
public class UserUpdate implements DateOfBirthInput, PhoneInput {
    private String login;

    private String imageId;

    /** True when the user asked to delete the current avatar (and uploaded no replacement). */
    private boolean removeImage;

    @NotBlank(message = "{user.firstName.required}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{user.firstName.pattern}")
    @Size(max = 50, message = "{user.firstName.size}")
    private String firstName;

    @Size(max = 50, message = "{user.lastName.size}")
    private String lastName;

    @NotBlank(message = "{user.email.required}")
    @Email(message = "{user.email.invalid}")
    @Size(max = 100, message = "{user.email.size}")
    private String email;

    /** The country chosen in the phone code list; not needed for a number typed with +. */
    private String phoneRegion;

    /** The number as typed; checked and converted by PhoneNumbers.resolve. */
    @Size(max = 30, message = "{user.phone.size}")
    private String phoneNumber;

    /** The checked number in E.164, set by PhoneNumbers.resolve. */
    private String phone;

    /** Who sees the number; absent while it is unconfirmed (the form disables the choice). */
    private ContactVisibility phoneVisibility;

    /** Who sees the address; absent while it is unconfirmed (the form disables the choice). */
    private ContactVisibility emailVisibility;

    /** Raw date-of-birth fields as typed; assembled by DateOfBirthResolver. */
    private String day;
    private String month;
    private String year;

    @Past(message = "{user.dateOfBirth.past}")
    private LocalDateTime dateOfBirth;

    private String countryCode;
    private String region;
    private String city;
    private String education;
    private String workplace;

    /**
     * The form filled with a user's stored profile. The date of birth is
     * split into the three fields the form shows; an absent date leaves them
     * empty.
     * @param user the stored user
     * @return the filled form
     */
    public static UserUpdate from(User user) {
        UserUpdate form = new UserUpdate();
        form.setLogin(user.getLogin());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        if (user.getPhone() != null) {
            form.setPhoneRegion(PhoneNumbers.regionOf(user.getPhone()));
            form.setPhoneNumber(PhoneNumbers.formatWithoutCountryCode(user.getPhone()));
        } else {
            // An old number that could not be read: shown as typed, with the
            // profile's country as the likely code, for the user to correct.
            form.setPhoneRegion(user.getCountry() == null ? null : user.getCountry().getCode());
            form.setPhoneNumber(user.getPhoneLegacy());
        }
        form.setPhoneVisibility(user.getPhoneVisibility());
        form.setEmailVisibility(user.getEmailVisibility());
        LocalDateTime dateOfBirth = user.getDateOfBirth();
        if (dateOfBirth != null) {
            form.setDay(String.valueOf(dateOfBirth.getDayOfMonth()));
            form.setMonth(String.valueOf(dateOfBirth.getMonthValue()));
            form.setYear(String.valueOf(dateOfBirth.getYear()));
        }
        form.setCountryCode(user.getCountry() == null ? null : user.getCountry().getCode());
        form.setRegion(user.getRegion());
        form.setCity(user.getCity());
        form.setEducation(user.getEducation());
        form.setWorkplace(user.getWorkplace());
        return form;
    }
}

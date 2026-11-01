package com.socialpath.dto.request;

import com.socialpath.entity.User;
import com.socialpath.validation.PhoneInput;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Raw registration input. This is where constraints on user-typed values
 * live — most importantly the 8–20 character rule for the plaintext
 * password, which must never sit on the entity: the entity stores a BCrypt
 * hash (~60 characters) and is validated again by JPA at persist time.
 * Property names match the registration form's field names, so the form
 * binds without configuration; {@link #toUser()} builds the entity.
 */
@Data
public class RegistrationForm implements DateOfBirthInput, PhoneInput {
    @NotBlank(message = "{user.login.required}")
    @Size(max = 20, message = "{user.login.size}")
    private String login;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 20, message = "{user.password.size}")
    private String password;

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
     * The new account from the checked form. The password is still plain
     * text here (it is hashed when the user is saved) and the country is
     * looked up by the caller.
     * @return an unsaved user
     */
    public User toUser() {
        User user = new User();
        user.setLogin(login);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setDateOfBirth(dateOfBirth);
        user.setRegion(region);
        user.setCity(city);
        user.setEducation(education);
        user.setWorkplace(workplace);
        return user;
    }
}

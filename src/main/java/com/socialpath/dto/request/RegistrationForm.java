package com.socialpath.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Raw registration input. This is where constraints on user-typed values
 * live — most importantly the 8–20 character rule for the plaintext
 * password, which must never sit on the entity: the entity stores a BCrypt
 * hash (~60 characters) and is validated again by JPA at persist time.
 * Property names deliberately match the registration form's field names and
 * the {@link com.socialpath.entity.User} entity, so both the form binding
 * and the ModelMapper conversion work without configuration.
 */
@Data
public class RegistrationForm {
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

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = "^[\\d\\s()+-]+$", message = "{user.phone.pattern}")
    @Size(max = 20, message = "{user.phone.size}")
    private String phoneNumber;

    @Past(message = "{user.dateOfBirth.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateOfBirth;

    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
}

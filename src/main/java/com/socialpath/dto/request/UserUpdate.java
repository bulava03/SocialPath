package com.socialpath.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserUpdate implements DateOfBirthInput {
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

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = "^[\\d\\s()+-]+$", message = "{user.phone.pattern}")
    @Size(max = 20, message = "{user.phone.size}")
    private String phoneNumber;

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
}

package com.socialpath.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserUpdate {
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

    @Past(message = "{user.dateOfBirth.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dateOfBirth;

    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;
    private List<ObjectId> groups;
    private List<String> friends;
    private List<String> friendInvites;
    private List<ObjectId> publications;
}

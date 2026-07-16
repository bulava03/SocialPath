package com.socialpath.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A user account. The login doubles as the primary key: it is immutable,
 * already embedded in JWTs and URLs, and every relation in the schema
 * references it directly.
 *
 * Friendship is stored as mirrored rows in the friendships table (one row per
 * direction), which keeps membership checks a simple contains() on either
 * side — the same invariant the service layer maintained in the MongoDB
 * edition. Group membership and the user's publications are NOT duplicated
 * here: they live in group_members and publications respectively and are
 * queried from there (single source of truth instead of the document model's
 * manually synchronized arrays).
 */
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @NotBlank(message = "{user.login.required}")
    @Size(max = 20, message = "{user.login.size}")
    @Column(name = "login", length = 20, nullable = false)
    private String login;

    /**
     * BCrypt hash of the password. The 8–20 character rule for the raw
     * password lives on {@link com.socialpath.dto.request.RegistrationForm};
     * here the constraint matches what is actually stored, so JPA's
     * persist-time validation can stay enabled.
     */
    @NotBlank
    @Size(max = 72)
    @Column(name = "password", length = 72, nullable = false)
    private String password;

    @Column(name = "image_id", length = 64)
    private String imageId;

    @NotBlank(message = "{user.firstName.required}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{user.firstName.pattern}")
    @Size(max = 50, message = "{user.firstName.size}")
    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Size(max = 50, message = "{user.lastName.size}")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @NotBlank(message = "{user.email.required}")
    @Email(message = "{user.email.invalid}")
    @Size(max = 100, message = "{user.email.size}")
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = "^[\\d\\s()+-]+$", message = "{user.phone.pattern}")
    @Size(max = 20, message = "{user.phone.size}")
    @Column(name = "phone_number", length = 20, nullable = false)
    private String phoneNumber;

    @Past(message = "{user.dateOfBirth.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    private String country;
    private String region;
    private String city;
    private String education;
    private String workplace;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "ban")
    private LocalDateTime ban;

    /**
     * Logins of confirmed friends. EAGER on purpose: the list is small, is
     * read by templates and helpers outside any transaction, and matches the
     * "whole document" loading semantics the rest of the code was written for.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "friendships", joinColumns = @JoinColumn(name = "user_login"))
    @Column(name = "friend_login", length = 20, nullable = false)
    private List<String> friends;

    /** Logins of users who invited this user to be friends. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "friend_invites", joinColumns = @JoinColumn(name = "user_login"))
    @Column(name = "inviter_login", length = 20, nullable = false)
    private List<String> friendInvites;
}

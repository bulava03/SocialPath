package com.socialpath.entity;

import com.socialpath.validation.PhoneNumbers;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    /**
     * The phone number in E.164 (+12015550123). Null only for a number kept
     * from before numbers were checked, which then sits in phoneLegacy.
     */
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$")
    @Column(name = "phone", length = 16)
    private String phone;

    /**
     * A number entered before numbers were checked that could not be read
     * (no country code and no country to guess it from), kept as typed until
     * the user saves the profile again. Null for everyone else.
     */
    @Column(name = "phone_legacy", length = 20)
    private String phoneLegacy;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_visibility", length = 10, nullable = false)
    private PhoneVisibility phoneVisibility = PhoneVisibility.FRIENDS;

    /**
     * Whether the owner entered the code sent to the number. Only a
     * confirmed number signs in, and only one account can confirm a number
     * (a unique key on the verified_phone column).
     */
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    /**
     * When the registration was sent, while it still waits for its phone
     * code; null for a finished account. A pending account cannot sign in,
     * is not found by search and is removed after a day.
     */
    @Column(name = "pending_since")
    private LocalDateTime pendingSince;

    @Past(message = "{user.dateOfBirth.past}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @ManyToOne
    @JoinColumn(name = "country_code")
    private Country country;

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
    private List<String> friends = new ArrayList<>();

    /** Logins of users who invited this user to be friends. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "friend_invites", joinColumns = @JoinColumn(name = "user_login"))
    @Column(name = "inviter_login", length = 20, nullable = false)
    private List<String> friendInvites = new ArrayList<>();

    /**
     * Whether someone may see this user's phone number.
     * @param viewerLogin who is looking
     * @return true for the user themselves and for whoever the visibility allows
     */
    public boolean phoneVisibleTo(String viewerLogin) {
        return login.equals(viewerLogin)
                || phoneVisibility == PhoneVisibility.EVERYONE
                || phoneVisibility == PhoneVisibility.FRIENDS && friends.contains(viewerLogin);
    }

    /**
     * The number as shown on the page.
     * @return grouped with its country code, or the old text as it was typed
     */
    public String getPhoneForDisplay() {
        return phone != null ? PhoneNumbers.formatInternational(phone) : phoneLegacy;
    }
}

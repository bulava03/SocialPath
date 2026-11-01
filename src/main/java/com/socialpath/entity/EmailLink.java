package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * The confirmation link last sent to a user's email address, one per user.
 * Only a hash of the link's token is stored: the token itself is only in the
 * letter.
 */
@Data
@Entity
@Table(name = "email_links")
@NoArgsConstructor
public class EmailLink {

    @Id
    @Column(name = "user_login", length = 20, nullable = false)
    private String userLogin;

    /** SHA-256 of the token, in hex. */
    @Column(name = "token_hash", length = 64, nullable = false, unique = true)
    private String tokenHash;

    /** The address the link was sent to. */
    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}

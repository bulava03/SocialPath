package com.socialpath.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The confirmation code last sent to a user's phone. A user has at most one:
 * sending again replaces the code but keeps the id, which the confirmation
 * page's address carries before the user can sign in.
 */
@Data
@Entity
@Table(name = "phone_codes")
@NoArgsConstructor
public class PhoneCode {

    /** Random, so the confirmation page of one registration cannot be guessed from another. */
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_login", length = 20, nullable = false, unique = true)
    private String userLogin;

    /** The number the code was sent to, in E.164. */
    @Column(name = "phone", length = 16, nullable = false)
    private String phone;

    /** BCrypt hash of the code; the code itself is only in the message. */
    @Column(name = "code_hash", length = 72, nullable = false)
    private String codeHash;

    /** Wrong codes entered since this one was sent. */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * A code for a user that has none yet.
     * @param userLogin whose number it confirms
     */
    public PhoneCode(String userLogin) {
        this.id = UUID.randomUUID().toString();
        this.userLogin = userLogin;
    }
}

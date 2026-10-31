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
 * A signed-in browser. The session token (JWT) carries the id, and the token
 * is accepted only while this row exists and has not expired, so deleting the
 * row signs that browser out at once.
 */
@Data
@Entity
@Table(name = "user_sessions")
@NoArgsConstructor
public class UserSession {

    /** Longest User-Agent kept; real ones are well under it. */
    public static final int USER_AGENT_LENGTH = 255;

    /** Random, so one session id cannot be guessed from another. */
    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_login", length = 20, nullable = false)
    private String userLogin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** The last request made with this session, to the refresh interval. */
    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    /** Moves forward while the session is used. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** The address the last request came from. */
    @Column(name = "ip", length = 45, nullable = false)
    private String ip;

    @Column(name = "user_agent", length = USER_AGENT_LENGTH)
    private String userAgent;

    /** Where the IP is, e.g. "Northtown, Exampleland"; null when unknown. */
    @Column(name = "location", length = 150)
    private String location;

    /**
     * A session opened just now.
     * @param userLogin whose session it is
     * @param ip where the sign-in came from
     * @param userAgent the browser's User-Agent header, may be null
     * @param location where the IP is, may be null
     * @param now the current time
     * @param expiresAt when it ends unless used
     */
    public UserSession(String userLogin, String ip, String userAgent, String location,
                       LocalDateTime now, LocalDateTime expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.userLogin = userLogin;
        this.ip = ip;
        this.userAgent = userAgent == null || userAgent.length() <= USER_AGENT_LENGTH
                ? userAgent : userAgent.substring(0, USER_AGENT_LENGTH);
        this.location = location;
        this.createdAt = now;
        this.lastSeenAt = now;
        this.expiresAt = expiresAt;
    }
}

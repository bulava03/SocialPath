package com.socialpath.security;

import com.socialpath.entity.UserSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * The cookie that carries the session token. Its flags are the security:
 * HttpOnly keeps scripts from reading it, SameSite=Strict is the CSRF
 * protection (the CSRF filter is off, see SecurityConfig), Secure keeps it
 * off plain HTTP.
 */
@Component
public class AuthCookie {

    private final JwtTokenProvider jwtTokenProvider;
    private final String name;

    public AuthCookie(JwtTokenProvider jwtTokenProvider, @Value("${jwt.cookie-name}") String name) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.name = name;
    }

    /**
     * A Set-Cookie value with a fresh token for the session; token and cookie
     * both last until the session's current expiry.
     * @param session the session to sign in with
     * @return the header value
     */
    public String issue(UserSession session) {
        Instant expiresAt = session.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant();
        String token = jwtTokenProvider.createToken(session.getUserLogin(), session.getId(), expiresAt);
        return build(token, Duration.between(Instant.now(), expiresAt));
    }

    /**
     * A Set-Cookie value that deletes the cookie.
     * @return the header value
     */
    public String clear() {
        return build("", Duration.ZERO);
    }

    private String build(String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}

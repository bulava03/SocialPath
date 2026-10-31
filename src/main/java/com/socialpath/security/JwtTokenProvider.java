package com.socialpath.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the JWT used as the session token. The token is stored
 * in an HttpOnly cookie set by the server (see AuthCookie), never touched by
 * front-end JavaScript. Besides the login it names the server-side session
 * (see SessionService), which is what can be ended before the token expires.
 */
@Component
public class JwtTokenProvider {

    /** The claim holding the session id. */
    static final String SESSION_CLAIM = "sid";

    private final UserDetailsService userDetailsService;
    private final SecretKey key;
    private final String cookieName;

    /**
     * What a valid token says.
     * @param login the user
     * @param sessionId the session it was issued for; null in tokens from
     *        before sessions existed, which no session matches
     */
    public record TokenClaims(String login, String sessionId) {
    }

    public JwtTokenProvider(@Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService,
                            @Value("${jwt.secret}") String secret,
                            @Value("${jwt.cookie-name}") String cookieName) {
        this.userDetailsService = userDetailsService;
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.cookieName = cookieName;
    }

    /**
     * Creates a signed token for a session.
     * @param login the user, stored as the subject
     * @param sessionId the session the token belongs to
     * @param expiresAt when the token stops being accepted
     * @return compact JWT string
     */
    public String createToken(String login, String sessionId, Instant expiresAt) {
        return Jwts.builder()
                .subject(login)
                .claim(SESSION_CLAIM, sessionId)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
    }

    /**
     * Checks the signature and expiry and reads the token.
     * @param token compact JWT string
     * @return the login and session id
     * @throws JwtAuthenticationException when the token is malformed, forged or expired
     */
    public TokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new TokenClaims(claims.getSubject(), claims.get(SESSION_CLAIM, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT token is expired or invalid", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Builds an Authentication for a user, refusing banned accounts.
     * @param login the login from a valid token
     * @return authentication with the user's details and authorities
     */
    public Authentication getAuthentication(String login) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(login);
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            throw new JwtAuthenticationException("Account is banned", HttpStatus.FORBIDDEN);
        }
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * Reads the token from the auth cookie, if present.
     * @param request current HTTP request
     * @return raw token or null when the cookie is absent
     */
    public String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

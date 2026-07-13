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
import java.util.Date;

/**
 * Issues and validates the JWT used as the session token. The token is stored
 * in an HttpOnly cookie set by the server (see AuthController), never touched
 * by front-end JavaScript.
 */
@Component
public class JwtTokenProvider {

    private final UserDetailsService userDetailsService;
    private final SecretKey key;
    private final long validityInMilliseconds;
    private final String cookieName;

    public JwtTokenProvider(@Qualifier("userDetailsServiceImpl") UserDetailsService userDetailsService,
                            @Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long validityInMilliseconds,
                            @Value("${jwt.cookie-name}") String cookieName) {
        this.userDetailsService = userDetailsService;
        this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
        this.cookieName = cookieName;
    }

    /**
     * Creates a signed token whose subject is the user's login.
     * @param username the login to embed as the subject
     * @return compact JWT string
     */
    public String createToken(String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Validates signature and expiration.
     * @param token compact JWT string
     * @return true when the token is well-formed, signed by us and not expired
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT token is expired or invalid", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Builds an Authentication for the token's subject, refusing banned accounts.
     * @param token compact JWT string
     * @return authentication with the user's details and authorities
     */
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(getUsername(token));
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            throw new JwtAuthenticationException("Account is banned", HttpStatus.FORBIDDEN);
        }
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * Extracts the login (subject) from a token.
     * @param token compact JWT string
     * @return the login stored in the subject claim
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
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

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

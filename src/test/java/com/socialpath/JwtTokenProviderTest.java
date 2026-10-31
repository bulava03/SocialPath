package com.socialpath;

import com.socialpath.security.JwtAuthenticationException;
import com.socialpath.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    private UserDetailsService userDetailsService;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        userDetailsService = mock(UserDetailsService.class);
        provider = new JwtTokenProvider(userDetailsService, SECRET, "token");
    }

    private static Instant inAnHour() {
        return Instant.now().plus(1, ChronoUnit.HOURS);
    }

    @Test
    void createToken_ThenParse_ReturnsLoginAndSession() {
        String token = provider.createToken("alice", "session-1", inAnHour());

        JwtTokenProvider.TokenClaims claims = provider.parse(token);

        assertEquals("alice", claims.login());
        assertEquals("session-1", claims.sessionId());
    }

    @Test
    void parse_WithExpiredToken_Throws() {
        String token = provider.createToken("alice", "session-1", Instant.now().minusSeconds(1));

        assertThrows(JwtAuthenticationException.class, () -> provider.parse(token));
    }

    @Test
    void parse_WithGarbage_Throws() {
        assertThrows(JwtAuthenticationException.class, () -> provider.parse("not-a-jwt"));
    }

    @Test
    void parse_SignedWithDifferentKey_Throws() {
        JwtTokenProvider other = new JwtTokenProvider(userDetailsService,
                "another-secret-key-0123456789abcdef0123456789abcdef", "token");
        String token = other.createToken("alice", "session-1", inAnHour());

        assertThrows(JwtAuthenticationException.class, () -> provider.parse(token));
    }

    @Test
    void parse_TokenFromBeforeSessions_HasNoSession() {
        // Such a token names no session, so the filter finds none and refuses it.
        String token = Jwts.builder()
                .subject("alice")
                .expiration(Date.from(inAnHour()))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertNull(provider.parse(token).sessionId());
    }

    @Test
    void getAuthentication_WithActiveUser_ReturnsAuthentication() {
        UserDetails details = User.withUsername("alice").password("x").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(details);

        Authentication authentication = provider.getAuthentication("alice");

        assertEquals("alice", ((UserDetails) authentication.getPrincipal()).getUsername());
    }

    @Test
    void getAuthentication_WithBannedUser_Throws() {
        UserDetails details = User.withUsername("alice").password("x")
                .authorities("ROLE_USER").disabled(true).accountLocked(true).build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(details);

        assertThrows(JwtAuthenticationException.class, () -> provider.getAuthentication("alice"));
    }
}

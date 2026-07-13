package com.socialpath;

import com.socialpath.security.JwtAuthenticationException;
import com.socialpath.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        provider = new JwtTokenProvider(userDetailsService, SECRET, 3_600_000L, "token");
    }

    @Test
    void createToken_ThenValidateAndReadSubject() {
        String token = provider.createToken("alice");

        assertTrue(provider.validateToken(token));
        assertEquals("alice", provider.getUsername(token));
    }

    @Test
    void validateToken_WithExpiredToken_Throws() {
        JwtTokenProvider expiring = new JwtTokenProvider(userDetailsService, SECRET, -1_000L, "token");
        String token = expiring.createToken("alice");

        assertThrows(JwtAuthenticationException.class, () -> expiring.validateToken(token));
    }

    @Test
    void validateToken_WithGarbage_Throws() {
        assertThrows(JwtAuthenticationException.class, () -> provider.validateToken("not-a-jwt"));
    }

    @Test
    void validateToken_SignedWithDifferentKey_Throws() {
        JwtTokenProvider other = new JwtTokenProvider(userDetailsService,
                "another-secret-key-0123456789abcdef0123456789abcdef", 3_600_000L, "token");
        String token = other.createToken("alice");

        assertThrows(JwtAuthenticationException.class, () -> provider.validateToken(token));
    }

    @Test
    void getAuthentication_WithActiveUser_ReturnsAuthentication() {
        UserDetails details = User.withUsername("alice").password("x").authorities("ROLE_USER").build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(details);

        String token = provider.createToken("alice");
        Authentication authentication = provider.getAuthentication(token);

        assertEquals("alice", ((UserDetails) authentication.getPrincipal()).getUsername());
    }

    @Test
    void getAuthentication_WithBannedUser_Throws() {
        UserDetails details = User.withUsername("alice").password("x")
                .authorities("ROLE_USER").disabled(true).accountLocked(true).build();
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(details);

        String token = provider.createToken("alice");

        assertThrows(JwtAuthenticationException.class, () -> provider.getAuthentication(token));
    }
}

package com.socialpath;

import com.socialpath.entity.UserSession;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.CurrentSession;
import com.socialpath.security.JwtTokenFilter;
import com.socialpath.security.JwtTokenProvider;
import com.socialpath.service.SessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A token is only as good as its session: a validly signed token whose session
 * was ended (logout, "end session", password change) no longer signs anyone in.
 */
class JwtTokenFilterTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    private JwtTokenProvider provider;
    private SessionService sessionService;
    private JwtTokenFilter filter;
    private UserSession session;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        UserDetailsService users = mock(UserDetailsService.class);
        when(users.loadUserByUsername("anna"))
                .thenReturn(User.withUsername("anna").password("x").authorities("ROLE_USER").build());
        provider = new JwtTokenProvider(users, SECRET, "token");
        sessionService = mock(SessionService.class);
        filter = new JwtTokenFilter(provider, sessionService, new AuthCookie(provider, "token"));

        LocalDateTime now = LocalDateTime.now();
        session = new UserSession("anna", "203.0.113.7", null, null, now, now.plusDays(7));
        request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.setCookies(new Cookie("token",
                provider.createToken("anna", session.getId(), Instant.now().plusSeconds(3600))));
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void liveSession_SignsTheUserInAndNamesTheSession() throws Exception {
        when(sessionService.findActive(session.getId(), "anna")).thenReturn(Optional.of(session));

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("anna", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(session.getId(), request.getAttribute(CurrentSession.ATTRIBUTE));
        verify(sessionService).touch(session, "203.0.113.7");
    }

    @Test
    void endedSession_LeavesTheRequestAnonymous() throws Exception {
        when(sessionService.findActive(anyString(), anyString())).thenReturn(Optional.empty());

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(request.getAttribute(CurrentSession.ATTRIBUTE));
        verify(sessionService, never()).touch(any(), any());
    }

    @Test
    void extendedSession_SendsAFreshCookie() throws Exception {
        when(sessionService.findActive(session.getId(), "anna")).thenReturn(Optional.of(session));
        when(sessionService.touch(session, "203.0.113.7")).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookie);
        String token = cookie.substring("token=".length(), cookie.indexOf(';'));
        assertEquals(session.getId(), provider.parse(token).sessionId());
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"), cookie);
    }

    @Test
    void sessionNotDueForRefresh_SendsNoCookie() throws Exception {
        when(sessionService.findActive(session.getId(), "anna")).thenReturn(Optional.of(session));
        when(sessionService.touch(session, "203.0.113.7")).thenReturn(false);

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    void forgedToken_IsIgnored() throws Exception {
        request.setCookies(new Cookie("token", "forged"));

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(sessionService, never()).findActive(any(), any());
    }
}

package com.socialpath.security;

import com.socialpath.entity.UserSession;
import com.socialpath.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.Optional;

/**
 * Authenticates a request by its session token: the token must be validly
 * signed and its session must still exist. A session in use is extended, and
 * the browser then receives a token with the new expiry.
 */
@Component
public class JwtTokenFilter extends GenericFilterBean {
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final AuthCookie authCookie;

    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider, SessionService sessionService, AuthCookie authCookie) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.authCookie = authCookie;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String token = jwtTokenProvider.resolveToken(request);
        try {
            if (token != null) {
                authenticate(token, request, (HttpServletResponse) servletResponse);
            }
        } catch (JwtAuthenticationException e) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parse(token);
        Optional<UserSession> found = sessionService.findActive(claims.sessionId(), claims.login());
        if (found.isEmpty()) {
            // Ended, expired or from before sessions existed: the request stays anonymous.
            return;
        }
        UserSession session = found.get();
        Authentication authentication = jwtTokenProvider.getAuthentication(claims.login());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute(CurrentSession.ATTRIBUTE, session.getId());

        if (sessionService.touch(session, request.getRemoteAddr())) {
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie.issue(session));
        }
    }
}

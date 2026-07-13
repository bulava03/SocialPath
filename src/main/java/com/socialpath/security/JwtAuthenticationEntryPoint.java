package com.socialpath.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decides what an unauthenticated request receives. REST endpoints get a
 * 401 with a JSON error body (the front-end redirects to the login page on
 * 401); everything else is a page navigation and is redirected to the home
 * page for login.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String[] REST_PREFIXES = {
        "/friends", "/comments", "/groupMembership", "/auth"
    };

    /**
     * Paths that start with a REST prefix but are actually page/fragment
     * endpoints and must be redirected, not answered with JSON. /comments/view
     * renders an HTML fragment for the feed, so it belongs here.
     */
    private static final String[] MVC_EXCEPTIONS = {
        "/comments/view"
    };

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String path = request.getRequestURI();
        if (isRest(path)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
        } else {
            response.sendRedirect("/");
        }
    }

    private boolean isRest(String path) {
        for (String mvc : MVC_EXCEPTIONS) {
            if (path.startsWith(mvc)) {
                return false;
            }
        }
        for (String prefix : REST_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}

package com.socialpath.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Central access point for the identity of the currently authenticated user.
 *
 * Controllers previously read the JWT cookie by hand, decoded the login, and
 * branched on a null token. With the security filter chain populating the
 * SecurityContext, the login is available here instead. If a request reaches a
 * controller at all it has already passed authentication, so the login is
 * guaranteed to be present.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof String login) {
            return login;
        }
        return null;
    }
}

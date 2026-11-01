package com.socialpath.security;

import com.socialpath.entity.UserSession;
import com.socialpath.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * The last step of every way in - login, phone number, a just-confirmed
 * registration: opens a session for the browser and sets its auth cookie.
 */
@Component
@RequiredArgsConstructor
public class SignIn {

    /** Where a signed-in user lands. */
    public static final String HOME = "redirect:/user/authorisation";

    private final SessionService sessionService;
    private final AuthCookie authCookie;

    /**
     * Signs a user in whose identity has been checked.
     * @param login who signs in
     * @param request the sign-in request, for the address and browser
     * @param response receives the auth cookie
     * @return a redirect to the user's page
     */
    public String complete(String login, HttpServletRequest request, HttpServletResponse response) {
        UserSession session = sessionService.open(login, request.getRemoteAddr(),
                request.getHeader(HttpHeaders.USER_AGENT));
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.issue(session));
        return HOME;
    }
}

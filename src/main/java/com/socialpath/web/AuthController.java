package com.socialpath.web;

import com.socialpath.entity.User;
import com.socialpath.entity.UserSession;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.CurrentSession;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Login/logout, both submitted by plain HTML forms. The JWT never reaches
 * front-end JavaScript: it is set here as an HttpOnly, SameSite=Strict cookie
 * and cleared the same way. SameSite=Strict is also what protects the
 * cookie-based session against CSRF, which is why the Spring CSRF filter
 * stays disabled in SecurityConfig.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionService sessionService;
    private final AuthCookie authCookie;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates by login/password. On success opens a session, sets the
     * auth cookie and opens the user's page; otherwise shows the login form
     * again with an error and the login that was typed.
     * @param login the login from the form
     * @param password the password from the form
     * @param model the model for re-rendering the form
     * @param httpRequest the sign-in request, for the address and browser
     * @param response receives the auth cookie
     * @return a redirect to the user's page, or the login page
     */
    @PostMapping("/login")
    public String login(@RequestParam String login, @RequestParam String password, Model model,
                        HttpServletRequest httpRequest, HttpServletResponse response) {
        User user = userService.findByLogin(login);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("login", login);
            model.addAttribute("errorText", "Incorrect login or password");
            return "home/index";
        }

        UserSession session = sessionService.open(user.getLogin(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader(HttpHeaders.USER_AGENT));
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.issue(session));
        return "redirect:/user/authorisation";
    }

    /**
     * Logs out: ends the session, so its token stops working even if a copy
     * of it is still around, and expires the auth cookie.
     * @param sessionId the session the request was made with
     * @param response receives the expired cookie
     * @return a redirect to the start page
     */
    @PostMapping("/logout")
    public String logout(@RequestAttribute(name = CurrentSession.ATTRIBUTE, required = false) String sessionId,
                         HttpServletResponse response) {
        sessionService.end(sessionId);
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.clear());
        return "redirect:/";
    }
}

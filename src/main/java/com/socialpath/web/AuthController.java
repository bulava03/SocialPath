package com.socialpath.web;

import com.socialpath.entity.User;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.security.JwtTokenProvider;
import com.socialpath.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;

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
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.cookie-name}")
    private String cookieName;
    @Value("${jwt.expiration}")
    private long tokenValidityMs;

    /**
     * Authenticates by login/password. On success sets the auth cookie and
     * opens the user's page; otherwise shows the login form again with an
     * error and the login that was typed.
     * @param request login and password from the form
     * @param model the model for re-rendering the form
     * @param response receives the auth cookie
     * @return a redirect to the user's page, or the login page
     */
    @PostMapping("/login")
    public String login(UserLogin request, Model model, HttpServletResponse response) {
        User user = userService.findByLogin(request.getLogin());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            model.addAttribute("login", request.getLogin());
            model.addAttribute("errorText", "Incorrect login or password");
            return "home/index";
        }

        String token = jwtTokenProvider.createToken(user.getLogin());
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie(token, Duration.ofMillis(tokenValidityMs)));
        return "redirect:/user/authorisation";
    }

    /**
     * Logs out by expiring the auth cookie.
     * @param response receives the expired cookie
     * @return a redirect to the start page
     */
    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie("", Duration.ZERO));
        return "redirect:/";
    }

    private String authCookie(String value, Duration maxAge) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}

package com.socialpath.web.rest;

import com.socialpath.entity.User;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.security.JwtTokenProvider;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Login/logout endpoints. The JWT never reaches front-end JavaScript: it is
 * set here as an HttpOnly, SameSite=Strict cookie and cleared the same way.
 * SameSite=Strict is also what protects the cookie-based session against
 * CSRF, which is why the Spring CSRF filter stays disabled in SecurityConfig.
 */
@RestController
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
     * Authenticates by login/password and sets the auth cookie.
     * @param request login and password from the form
     * @return 204 with Set-Cookie on success, 401 otherwise
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(UserLogin request) {
        User user = userService.findByLogin(request.getLogin());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenProvider.createToken(user.getLogin());
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofMillis(tokenValidityMs))
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Logs out by expiring the auth cookie.
     * @return 204 with an expired Set-Cookie header
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}

package com.socialpath;

import com.socialpath.dto.response.UserLogin;
import com.socialpath.entity.User;
import com.socialpath.security.JwtTokenProvider;
import com.socialpath.service.UserService;
import com.socialpath.web.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The session lives in the auth cookie, so its flags are the security: HttpOnly
 * keeps scripts from reading it, SameSite=Strict is the CSRF protection (the
 * CSRF filter is off), Secure keeps it off plain HTTP.
 */
class AuthControllerTest {

    private UserService userService;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private AuthController controller;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        passwordEncoder = mock(PasswordEncoder.class);
        controller = new AuthController(userService, jwtTokenProvider, passwordEncoder);
        ReflectionTestUtils.setField(controller, "cookieName", "token");
        ReflectionTestUtils.setField(controller, "tokenValidityMs", 3_600_000L);
        response = new MockHttpServletResponse();

        User anna = new User();
        anna.setLogin("anna");
        anna.setPassword("hash");
        when(userService.findByLogin("anna")).thenReturn(anna);
        when(passwordEncoder.matches("right", "hash")).thenReturn(true);
        when(jwtTokenProvider.createToken("anna")).thenReturn("jwt-value");
    }

    @Test
    void login_RightPassword_SetsAProtectedSessionCookie() {
        String view = controller.login(new UserLogin("anna", "right"), new ExtendedModelMap(), response);

        assertEquals("redirect:/user/authorisation", view);
        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.startsWith("token=jwt-value;"), cookie);
        assertTrue(cookie.contains("HttpOnly"), cookie);
        assertTrue(cookie.contains("Secure"), cookie);
        assertTrue(cookie.contains("SameSite=Strict"), cookie);
        assertTrue(cookie.contains("Path=/"), cookie);
        assertTrue(cookie.contains("Max-Age=3600"), cookie);
    }

    @Test
    void login_WrongPassword_ShowsTheFormAgainWithoutACookie() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.login(new UserLogin("anna", "wrong"), model, response);

        assertEquals("home/index", view);
        assertEquals("anna", model.get("login"));
        assertEquals("Incorrect login or password", model.get("errorText"));
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        verify(jwtTokenProvider, never()).createToken(anyString());
    }

    @Test
    void login_UnknownUser_GetsTheSameMessageAsAWrongPassword() {
        // Telling "no such user" apart from "wrong password" would reveal which logins exist.
        ExtendedModelMap model = new ExtendedModelMap();

        controller.login(new UserLogin("nobody", "whatever"), model, response);

        assertEquals("Incorrect login or password", model.get("errorText"));
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    void logout_ExpiresTheCookieWithTheSameFlags() {
        String view = controller.logout(response);

        assertEquals("redirect:/", view);
        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.startsWith("token=;"), cookie);
        assertTrue(cookie.contains("Max-Age=0"), cookie);
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"), cookie);
    }
}

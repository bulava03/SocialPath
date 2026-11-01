package com.socialpath;

import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.entity.UserSession;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.JwtTokenProvider;
import com.socialpath.security.SignIn;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
import com.socialpath.web.AuthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    private UserService userService;
    private SessionService sessionService;
    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;
    private PhoneVerificationService phoneVerification;
    private AuthController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        sessionService = mock(SessionService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = new JwtTokenProvider(mock(UserDetailsService.class), SECRET, "token");
        phoneVerification = mock(PhoneVerificationService.class);
        AuthCookie authCookie = new AuthCookie(jwtTokenProvider, "token");
        controller = new AuthController(userService, sessionService, authCookie, passwordEncoder,
                new SignIn(sessionService, authCookie), phoneVerification);
        request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 Firefox/130.0");
        response = new MockHttpServletResponse();

        User anna = new User();
        anna.setLogin("anna");
        anna.setPassword("hash");
        when(userService.findByLogin("anna")).thenReturn(anna);
        when(userService.findByVerifiedPhone("+12015550123")).thenReturn(anna);
        when(passwordEncoder.matches("right", "hash")).thenReturn(true);
        LocalDateTime now = LocalDateTime.now();
        when(sessionService.open("anna", "203.0.113.7", "Mozilla/5.0 Firefox/130.0"))
                .thenReturn(new UserSession("anna", "203.0.113.7", null, null, now, now.plusHours(1)));
    }

    @Test
    void login_RightPassword_OpensASessionAndSetsAProtectedCookieForIt() {
        String view = controller.login("anna", "right", new ExtendedModelMap(), request, response);

        assertEquals("redirect:/user/authorisation", view);
        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.startsWith("token="), cookie);
        assertTrue(cookie.contains("HttpOnly"), cookie);
        assertTrue(cookie.contains("Secure"), cookie);
        assertTrue(cookie.contains("SameSite=Strict"), cookie);
        assertTrue(cookie.contains("Path=/"), cookie);
        // The cookie lasts as long as the session: an hour here, give or take the test's own run time.
        assertTrue(cookie.contains("Max-Age=3599") || cookie.contains("Max-Age=3600"), cookie);

        String token = cookie.substring("token=".length(), cookie.indexOf(';'));
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parse(token);
        assertEquals("anna", claims.login());
        verify(sessionService).open("anna", "203.0.113.7", "Mozilla/5.0 Firefox/130.0");
    }

    @Test
    void login_WrongPassword_ShowsTheFormAgainWithoutASession() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.login("anna", "wrong", model, request, response);

        assertEquals("home/index", view);
        assertEquals("anna", model.get("login"));
        assertEquals("Incorrect login or password", model.get("errorText"));
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        verify(sessionService, never()).open(anyString(), anyString(), any());
    }

    @Test
    void login_UnknownUser_GetsTheSameMessageAsAWrongPassword() {
        // Telling "no such user" apart from "wrong password" would reveal which logins exist.
        ExtendedModelMap model = new ExtendedModelMap();

        controller.login("nobody", "whatever", model, request, response);

        assertEquals("Incorrect login or password", model.get("errorText"));
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_ConfirmedPhoneNumberInTheLoginField_SignsInTheNumbersAccount() {
        // Any international spelling of the number: spaces, dashes, brackets.
        for (String typed : new String[] {"+12015550123", "+1 201-555-0123", " +1 (201) 555 0123 "}) {
            MockHttpServletResponse signedIn = new MockHttpServletResponse();

            String view = controller.login(typed, "right", new ExtendedModelMap(), request, signedIn);

            assertEquals("redirect:/user/authorisation", view, typed);
            assertTrue(signedIn.getHeader(HttpHeaders.SET_COOKIE).startsWith("token="), typed);
        }
    }

    @Test
    void login_UnconfirmedOrUnknownNumber_GetsTheSameMessageAsAWrongPassword() {
        // An unconfirmed number is not found by findByVerifiedPhone, the same as one nobody has.
        for (String[] attempt : new String[][] {{"+1 201 555 0199", "right"}, {"+1 201 555 0123", "wrong"},
                                                 {"+not a number", "right"}}) {
            ExtendedModelMap model = new ExtendedModelMap();

            String view = controller.login(attempt[0], attempt[1], model, request, response);

            assertEquals("home/index", view);
            assertEquals(attempt[0], model.get("login"));
            assertEquals("Incorrect login or password", model.get("errorText"));
        }
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        verify(sessionService, never()).open(anyString(), anyString(), any());
    }

    @Test
    void login_RegistrationNotConfirmed_SendsACodeInsteadOfSigningIn() {
        User pending = new User();
        pending.setLogin("newbie");
        pending.setPassword("hash");
        pending.setPendingSince(LocalDateTime.now());
        when(userService.findByLogin("newbie")).thenReturn(pending);
        PhoneCode code = new PhoneCode("newbie");
        when(phoneVerification.requestCode(pending)).thenReturn(code);

        String view = controller.login("newbie", "right", new ExtendedModelMap(), request, response);

        assertEquals("redirect:/registration/confirm?id=" + code.getId(), view);
        assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
        verify(sessionService, never()).open(anyString(), anyString(), any());
    }

    @Test
    void logout_EndsTheSessionAndExpiresTheCookieWithTheSameFlags() {
        String view = controller.logout("session-1", response);

        assertEquals("redirect:/", view);
        verify(sessionService).end("session-1");
        String cookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cookie.startsWith("token=;"), cookie);
        assertTrue(cookie.contains("Max-Age=0"), cookie);
        assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Strict"), cookie);
    }
}

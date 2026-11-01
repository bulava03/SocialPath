package com.socialpath.web;

import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.CurrentSession;
import com.socialpath.security.SignIn;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
import com.socialpath.validation.PhoneNumbers;
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
 *
 * <p>The login field also takes a confirmed phone number in the
 * international format (+ and the country code) or a confirmed email
 * address, with the same password.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionService sessionService;
    private final AuthCookie authCookie;
    private final PasswordEncoder passwordEncoder;
    private final SignIn signIn;
    private final PhoneVerificationService phoneVerification;

    /**
     * Authenticates by login or phone number, and password. On success opens
     * a session, sets the auth cookie and opens the user's page; otherwise
     * shows the login form again with an error and what was typed.
     * @param login the login, a phone number starting with +, or an email address
     * @param password the password from the form
     * @param model the model for re-rendering the form
     * @param httpRequest the sign-in request, for the address and browser
     * @param response receives the auth cookie
     * @return a redirect to the user's page or the phone confirmation, or the login page
     */
    @PostMapping("/login")
    public String login(@RequestParam String login, @RequestParam String password, Model model,
                        HttpServletRequest httpRequest, HttpServletResponse response) {
        User user = findAccount(login);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            // One message for every case, so the form does not tell which logins or numbers have accounts.
            model.addAttribute("login", login);
            model.addAttribute("errorText", "Incorrect login or password");
            return "home/index";
        }
        if (user.getPendingSince() != null || !user.isPhoneVerified() && user.getPhone() != null) {
            // A registration still waiting for its phone code, or an account
            // from before numbers were confirmed: the number is confirmed
            // first. A code is sent (or the recent one kept).
            PhoneCode code = phoneVerification.requestCode(user);
            return "redirect:/registration/confirm?id=" + code.getId();
        }
        return signIn.complete(user.getLogin(), httpRequest, response);
    }

    /**
     * The account for what was typed in the login field. Something starting
     * with + is read as a phone number, something with @ as an email
     * address; either counts only once confirmed (an unconfirmed one may be
     * typed by several accounts). Anything else, or a number or address
     * nobody has confirmed, is looked up as a login.
     * @param typed the login field
     * @return the account, or null
     */
    private User findAccount(String typed) {
        String input = typed.strip();
        if (input.startsWith("+")) {
            User byPhone = PhoneNumbers.toE164(null, input).map(userService::findByVerifiedPhone).orElse(null);
            if (byPhone != null) {
                return byPhone;
            }
        } else if (input.contains("@")) {
            User byEmail = userService.findByVerifiedEmail(input);
            if (byEmail != null) {
                return byEmail;
            }
        }
        return userService.findByLogin(input);
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

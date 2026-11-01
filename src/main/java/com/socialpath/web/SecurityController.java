package com.socialpath.web;

import com.socialpath.config.MailInbox;
import com.socialpath.dto.request.ChangePasswordForm;
import com.socialpath.entity.EmailLink;
import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.CurrentSession;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.EmailVerificationService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.service.PhoneVerificationService.Outcome;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
import com.socialpath.sms.DemoSmsGateway;
import com.socialpath.validation.PhoneNumbers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

/**
 * The Security page, in four tabs: changing the password, the phone number
 * (confirming a new one), the email address (sending its confirmation link)
 * and the list of signed-in browsers, each of which can be signed out from
 * there. Each tab
 * has its own address, so every action returns to the tab it came from.
 */
@Controller
@RequestMapping("/security")
@RequiredArgsConstructor
public class SecurityController {

    private final UserService userService;
    private final SessionService sessionService;
    private final HandleAvatarService handleAvatarService;
    private final AuthCookie authCookie;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerification;
    private final DemoSmsGateway demoSms;
    private final EmailVerificationService emailVerification;
    private final MailInbox mailInbox;

    /** The tab that shows the password form. */
    static final String PASSWORD_TAB = "password";
    /** The tab that lists the sessions. */
    static final String SESSIONS_TAB = "sessions";
    /** The tab that confirms the phone number. */
    static final String PHONE_TAB = "phone";
    /** The tab that confirms the email address. */
    static final String EMAIL_TAB = "email";

    @GetMapping
    public String securityPage() {
        return "redirect:/security/" + PASSWORD_TAB;
    }

    @GetMapping("/" + PASSWORD_TAB)
    public String passwordTab(@CurrentUser User user,
                              @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        return page(user, sessionId, model, PASSWORD_TAB, "");
    }

    @GetMapping("/" + PHONE_TAB)
    public String phoneTab(@CurrentUser User user,
                           @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        return page(user, sessionId, model, PHONE_TAB, "");
    }

    @GetMapping("/" + EMAIL_TAB)
    public String emailTab(@CurrentUser User user,
                           @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        return page(user, sessionId, model, EMAIL_TAB, "");
    }

    @GetMapping("/" + SESSIONS_TAB)
    public String sessionsTab(@CurrentUser User user,
                              @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        return page(user, sessionId, model, SESSIONS_TAB, "");
    }

    /**
     * Changes the password and ends every other session, so whoever may have
     * known the old password is signed out everywhere; this browser stays in.
     * @param user the signed-in user
     * @param sessionId the session the request was made with, which stays
     * @param form the old password and the new one, typed twice
     * @param bindingResult validation of the form
     * @param model the model for showing the page again with an error
     * @return the password tab with an error, or a redirect to it with a confirmation
     */
    @PostMapping("/changePassword")
    public String changePassword(@CurrentUser User user, @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId,
                                 @Valid ChangePasswordForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return page(user, sessionId, model, PASSWORD_TAB, bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            return page(user, sessionId, model, PASSWORD_TAB, "The new passwords do not match.");
        }
        if (!passwordEncoder.matches(form.getOldPassword(), user.getPassword())) {
            return page(user, sessionId, model, PASSWORD_TAB, "The old password is incorrect.");
        }

        userService.updatePassword(user.getLogin(), form.getPassword());
        sessionService.endOthers(user.getLogin(), sessionId);
        return "redirect:/security/" + PASSWORD_TAB + "?passwordChanged";
    }

    /**
     * Signs one of the user's browsers out. Ending the session the request
     * came with works like logging out.
     * @param user the signed-in user
     * @param sessionId the session the request was made with
     * @param id the session to end
     * @param response receives the expired cookie when the current session ends
     * @return a redirect to the sessions tab, or to the start page
     */
    @PostMapping("/endSession")
    public String endSession(@CurrentUser User user, @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId,
                             @RequestParam String id, HttpServletResponse response) {
        sessionService.end(user.getLogin(), id);
        if (id.equals(sessionId)) {
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie.clear());
            return "redirect:/";
        }
        return "redirect:/security/" + SESSIONS_TAB;
    }

    @PostMapping("/endOtherSessions")
    public String endOtherSessions(@CurrentUser User user,
                                   @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId) {
        sessionService.endOthers(user.getLogin(), sessionId);
        return "redirect:/security/" + SESSIONS_TAB;
    }

    /**
     * Sends the code again: to the new number waiting for confirmation, or
     * to the profile's number while it is unconfirmed. Not sooner than
     * {@link PhoneVerificationService#RESEND_AFTER} after the last one.
     * @param user the signed-in user
     * @param sessionId the session the request was made with
     * @param model the model for showing the page again with an error
     * @return the phone tab, with a note on what happened
     */
    @PostMapping("/phone/sendCode")
    public String sendPhoneCode(@CurrentUser User user,
                                @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        Optional<PhoneCode> waiting = waitingCode(user);
        String target = waiting.map(PhoneCode::getPhone)
                .orElse(user.isPhoneVerified() ? null : user.getPhone());
        if (target == null) {
            return user.isPhoneVerified() ? "redirect:/security/" + PHONE_TAB
                    : page(user, sessionId, model, PHONE_TAB, "Enter the number in your profile first.");
        }
        if (waiting.isPresent() && phoneVerification.secondsUntilResend(waiting.get()) > 0) {
            return "redirect:/security/" + PHONE_TAB + "?wait";
        }
        phoneVerification.requestCode(user, target);
        return "redirect:/security/" + PHONE_TAB + "?sent";
    }

    /**
     * Checks the code; the right one makes the number it went to the
     * profile's confirmed number.
     * @param user the signed-in user
     * @param sessionId the session the request was made with
     * @param code what the user typed
     * @param model the model for showing the page again with an error
     * @return the phone tab with an error, or a redirect to it with a confirmation
     */
    @PostMapping("/phone/confirm")
    public String confirmPhone(@CurrentUser User user, @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId,
                               @RequestParam(required = false) String code, Model model) {
        Optional<PhoneCode> sent = waitingCode(user);
        if (sent.isEmpty()) {
            return page(user, sessionId, model, PHONE_TAB, "This code can no longer be used. Send a new one.");
        }
        Outcome outcome = phoneVerification.confirm(sent.get(), code);
        if (outcome == Outcome.CONFIRMED) {
            return "redirect:/security/" + PHONE_TAB + "?confirmed";
        }
        return page(user, sessionId, model, PHONE_TAB, PhoneCodeMessages.of(outcome, sent.get()));
    }

    /**
     * Sends a link to confirm the profile's email address, not sooner than
     * {@link EmailVerificationService#RESEND_AFTER} after the last one.
     * @param user the signed-in user
     * @param request the request, for the address in the link
     * @return the email tab, with a note on what happened
     */
    @PostMapping("/email/sendLink")
    public String sendEmailLink(@CurrentUser User user, HttpServletRequest request) {
        if (user.isEmailVerified()) {
            return "redirect:/security/" + EMAIL_TAB;
        }
        Optional<EmailLink> last = emailVerification.findFor(user.getLogin());
        if (last.isPresent() && emailVerification.secondsUntilResend(last.get()) > 0) {
            return "redirect:/security/" + EMAIL_TAB + "?wait";
        }
        boolean sent = emailVerification.sendLink(user, ServletUriComponentsBuilder.fromContextPath(request).toUriString());
        return "redirect:/security/" + EMAIL_TAB + (sent ? "?sent" : "?failed");
    }

    /**
     * The code waiting to be entered: for a new number from the profile, or
     * for the profile's number while it is unconfirmed.
     * @param user the signed-in user
     * @return the code, if one can still be entered
     */
    private Optional<PhoneCode> waitingCode(User user) {
        return phoneVerification.findWaiting(user.getLogin())
                .filter(code -> !user.isPhoneVerified() || !code.getPhone().equals(user.getPhone()));
    }

    // Every tab gets the sessions: the tab switcher shows how many there are.
    private String page(User user, String sessionId, Model model, String tab, String errorText) {
        handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("tab", tab);
        model.addAttribute("sessions", sessionService.list(user.getLogin(), sessionId));
        model.addAttribute("errorText", errorText);
        if (PHONE_TAB.equals(tab)) {
            model.addAttribute("phone", user.getPhoneForDisplay());
            model.addAttribute("phoneVerified", user.isPhoneVerified());
            Optional<PhoneCode> waiting = waitingCode(user);
            model.addAttribute("pendingPhone", waiting.map(code -> PhoneNumbers.formatInternational(code.getPhone()))
                    .orElse(null));
            model.addAttribute("resendIn", waiting.map(phoneVerification::secondsUntilResend).orElse(0L));
            model.addAttribute("demoInbox", demoSms.isInboxEnabled()
                    ? waiting.map(code -> DemoSmsController.inboxLink(code.getPhone())).orElse(null) : null);
        }
        if (EMAIL_TAB.equals(tab)) {
            model.addAttribute("email", user.getEmail());
            model.addAttribute("emailVerified", user.isEmailVerified());
            Optional<EmailLink> link = emailVerification.findFor(user.getLogin());
            model.addAttribute("resendIn", link.map(emailVerification::secondsUntilResend).orElse(0L));
            model.addAttribute("mailInbox", mailInbox.getUrl());
        }
        return "user/security";
    }
}

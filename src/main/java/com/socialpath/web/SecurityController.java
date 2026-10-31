package com.socialpath.web;

import com.socialpath.dto.request.ChangePasswordForm;
import com.socialpath.entity.User;
import com.socialpath.security.AuthCookie;
import com.socialpath.security.CurrentSession;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.SessionService;
import com.socialpath.service.UserService;
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

/**
 * The Security page, in two tabs: changing the password, and the list of
 * signed-in browsers, each of which can be signed out from there. Each tab
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

    /** The tab that shows the password form. */
    static final String PASSWORD_TAB = "password";
    /** The tab that lists the sessions. */
    static final String SESSIONS_TAB = "sessions";

    @GetMapping
    public String securityPage() {
        return "redirect:/security/" + PASSWORD_TAB;
    }

    @GetMapping("/" + PASSWORD_TAB)
    public String passwordTab(@CurrentUser User user,
                              @RequestAttribute(CurrentSession.ATTRIBUTE) String sessionId, Model model) {
        return page(user, sessionId, model, PASSWORD_TAB, "");
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

    // Both tabs get the sessions: the tab switcher shows how many there are.
    private String page(User user, String sessionId, Model model, String tab, String errorText) {
        handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("tab", tab);
        model.addAttribute("sessions", sessionService.list(user.getLogin(), sessionId));
        model.addAttribute("errorText", errorText);
        return "user/security";
    }
}

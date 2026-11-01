package com.socialpath.web;

import com.socialpath.service.EmailVerificationService;
import com.socialpath.service.EmailVerificationService.Outcome;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Where the link in a confirmation letter leads. It works without signing
 * in: the letter may be opened on another device, and the random token in
 * the link is the proof that the address is the user's.
 */
@Controller
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailVerificationService emailVerification;

    /**
     * Confirms the address the link was sent to and says how it went.
     * @param token the token from the link
     * @param model the model for the page
     * @return the result page
     */
    @GetMapping("/confirm")
    public String confirm(@RequestParam(required = false) String token, Model model) {
        Outcome outcome = emailVerification.confirm(token);
        model.addAttribute("confirmed", outcome == Outcome.CONFIRMED);
        model.addAttribute("message", switch (outcome) {
            case CONFIRMED -> "Your email is confirmed. You can sign in with it now.";
            case INVALID -> "This link does not work any more: it was used already, a newer one was sent, "
                    + "or it is more than 24 hours old. Send a new one from the Security page.";
            case EMAIL_TAKEN -> UserService.EMAIL_TAKEN;
        });
        return "email/confirmed";
    }
}

package com.socialpath.web;

import com.socialpath.entity.Country;
import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.dto.request.RegistrationForm;
import com.socialpath.security.SignIn;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.service.PhoneVerificationService.Outcome;
import com.socialpath.sms.DemoSmsGateway;
import com.socialpath.validation.PhoneNumbers;
import com.socialpath.validation.ValidationResult;
import com.socialpath.service.CountryService;
import com.socialpath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Registration flow. The form binds to {@link RegistrationForm} (raw input,
 * including the plaintext password with its own constraints) rather than to
 * the entity; the entity is created only after the input has been validated.
 *
 * <p>The account is then pending until the code sent to its phone number is
 * entered on the confirmation page; entering it signs the new user in. The
 * page is found by the code's random id, as the user cannot be signed in yet.
 */
@Controller
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final CountryService countryService;
    private final PhoneVerificationService phoneVerification;
    private final DemoSmsGateway demoSms;
    private final SignIn signIn;

    @ModelAttribute("countries")
    public List<Country> countries() {
        return countryService.findAll();
    }

    @ModelAttribute("phoneRegions")
    public List<PhoneNumbers.Region> phoneRegions() {
        return PhoneNumbers.regions(countryService.findAll());
    }

    @GetMapping()
    public String getRegistrationPage(Model model) {
        model.addAttribute("User", new RegistrationForm());
        return "registration/registration";
    }

    @PostMapping("/registration")
    public String registration(RegistrationForm form, Model model) {
        if (userService.findByLogin(form.getLogin()) != null) {
            model.addAttribute("User", form);
            model.addAttribute("errorText", "This login is already taken.");
            return "registration/registration";
        }

        ValidationResult validation = userService.validateRegistration(form);
        if (!validation.isSuccess()) {
            model.addAttribute("User", form);
            model.addAttribute("errorText", validation.getMessage());
            return "registration/registration";
        }

        User user = form.toUser();
        user.setCountry(countryService.resolve(form.getCountryCode()));
        user.setPendingSince(LocalDateTime.now());
        userService.addUser(user);

        PhoneCode code = phoneVerification.requestCode(user);
        return confirmPage(code.getId(), "");
    }

    /**
     * The page that asks for the code sent to the new account's phone.
     * @param id the code id from the address
     * @param model the model for the page
     * @return the confirmation page, or the start page when the registration is gone
     */
    @GetMapping("/confirm")
    public String confirmation(@RequestParam(required = false) String id, Model model) {
        return pendingCode(id)
                .map(code -> showConfirmation(code, model, ""))
                .orElse("redirect:/");
    }

    /**
     * Checks the code. The right one finishes the registration and signs the
     * user in, the browser that registered being the one holding the phone.
     * @param id the code id
     * @param code what the user typed
     * @param model the model for showing the page again with an error
     * @param request the request, for the session's address and browser
     * @param response receives the auth cookie
     * @return the user's page, or the confirmation page with an error
     */
    @PostMapping("/confirm")
    public String confirm(@RequestParam String id, @RequestParam(required = false) String code, Model model,
                          HttpServletRequest request, HttpServletResponse response) {
        Optional<PhoneCode> sent = pendingCode(id);
        if (sent.isEmpty()) {
            return "redirect:/";
        }
        Outcome outcome = phoneVerification.confirm(sent.get(), code);
        if (outcome == Outcome.CONFIRMED) {
            return signIn.complete(sent.get().getUserLogin(), request, response);
        }
        return showConfirmation(sent.get(), model, PhoneCodeMessages.of(outcome, sent.get()));
    }

    /**
     * Sends a new code, unless the last one went out too recently.
     * @param id the code id
     * @return the confirmation page, with a note on what happened
     */
    @PostMapping("/resend")
    public String resend(@RequestParam String id) {
        Optional<PhoneCode> sent = pendingCode(id);
        if (sent.isEmpty()) {
            return "redirect:/";
        }
        if (phoneVerification.secondsUntilResend(sent.get()) > 0) {
            return confirmPage(id, "&wait");
        }
        phoneVerification.requestCode(userService.findByLogin(sent.get().getUserLogin()));
        return confirmPage(id, "&resent");
    }

    /**
     * Drops a pending registration, e.g. to start over with another number,
     * and frees its login at once instead of after a day.
     * @param id the code id
     * @return the empty registration form
     */
    @PostMapping("/cancel")
    public String cancel(@RequestParam String id) {
        pendingCode(id).ifPresent(phoneVerification::cancelRegistration);
        return "redirect:/registration";
    }

    private String showConfirmation(PhoneCode code, Model model, String errorText) {
        model.addAttribute("codeId", code.getId());
        model.addAttribute("phone", PhoneNumbers.formatInternational(code.getPhone()));
        model.addAttribute("resendIn", phoneVerification.secondsUntilResend(code));
        model.addAttribute("demoInbox", demoSms.isInboxEnabled() ? DemoSmsController.inboxLink(code.getPhone()) : null);
        model.addAttribute("errorText", errorText);
        return "registration/confirm";
    }

    /**
     * The code of a registration that still waits for it. A code sent to a
     * finished account (from the Security page) is not accepted here: this
     * page signs in without the password.
     * @param id the code id from the request
     * @return the code, if its registration is still pending
     */
    private Optional<PhoneCode> pendingCode(String id) {
        return phoneVerification.find(id).filter(code -> {
            User user = userService.findByLogin(code.getUserLogin());
            return user != null && user.getPendingSince() != null;
        });
    }

    private static String confirmPage(String id, String note) {
        return "redirect:/registration/confirm?id=" + id + note;
    }
}

package com.socialpath.web;

import com.socialpath.entity.Country;
import com.socialpath.entity.User;
import com.socialpath.dto.request.RegistrationForm;
import com.socialpath.dto.response.UserFormView;
import com.socialpath.validation.ValidationResult;
import com.socialpath.service.CountryService;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Registration flow. The form binds to {@link RegistrationForm} (raw input,
 * including the plaintext password with its own constraints) rather than to
 * the entity; the entity is created only after the input has been validated.
 */
@Controller
@RequestMapping("/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final ModelMapper modelMapper;
    private final CountryService countryService;

    @ModelAttribute("countries")
    public List<Country> countries() {
        return countryService.findAll();
    }

    @GetMapping()
    public String getRegistrationPage(Model model) {
        model.addAttribute("User", new UserFormView());
        return "registration/registration";
    }

    @PostMapping("/registration")
    public String registration(RegistrationForm form, Model model) {
        if (userService.findByLogin(form.getLogin()) != null) {
            model.addAttribute("User", toFormView(form));
            model.addAttribute("errorText", "This login is already taken.");
            return "registration/registration";
        }

        ValidationResult validation = userService.validateRegistration(form);
        if (!validation.isSuccess()) {
            model.addAttribute("User", toFormView(form));
            model.addAttribute("errorText", validation.getMessage());
            return "registration/registration";
        }

        User user = modelMapper.map(form, User.class);
        user.setCountry(countryService.resolve(form.getCountryCode()));
        userService.addUser(user);

        return "redirect:/";
    }

    private UserFormView toFormView(RegistrationForm form) {
        UserFormView userForm = modelMapper.map(form, UserFormView.class);
        // Echo the raw fields back, so an invalid date is shown as typed.
        userForm.setDay(form.getDay());
        userForm.setMonth(form.getMonth());
        userForm.setYear(form.getYear());
        return userForm;
    }
}

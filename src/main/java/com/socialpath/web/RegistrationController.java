package com.socialpath.web;

import com.socialpath.entity.User;
import com.socialpath.dto.request.RegistrationForm;
import com.socialpath.dto.response.UserFormView;
import com.socialpath.helper.ConvertHelper;
import com.socialpath.validation.ValidationResult;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @GetMapping()
    public String getRegistrationPage(Model model) {
        UserFormView user = new UserFormView();
        user.setYear(-1);
        model.addAttribute("User", user);
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

        if (form.getDateOfBirth() != null) {
            form.setDateOfBirth(form.getDateOfBirth().toLocalDate().atStartOfDay());
        }
        User user = modelMapper.map(form, User.class);
        userService.addUser(user);

        return "redirect:/";
    }

    private UserFormView toFormView(RegistrationForm form) {
        UserFormView userForm = modelMapper.map(form, UserFormView.class);
        if (form.getDateOfBirth() != null) {
            userForm.setDay(form.getDateOfBirth().getDayOfMonth());
            userForm.setMonth(ConvertHelper.monthToString(form.getDateOfBirth().getMonthValue()));
            userForm.setYear(form.getDateOfBirth().getYear());
        } else {
            userForm.setYear(-1);
        }
        return userForm;
    }
}

package com.socialpath.web;

import com.socialpath.document.User;
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
    public String registration(User user, Model model) {
        if (userService.findByLogin(user.getLogin()) != null) {
            model.addAttribute("User", toFormView(user));
            model.addAttribute("errorText", "This login is already taken.");
            return "registration/registration";
        }

        ValidationResult validation = userService.validateUser(user);
        if (!validation.isSuccess()) {
            model.addAttribute("User", toFormView(user));
            model.addAttribute("errorText", validation.getMessage());
            return "registration/registration";
        }

        if (user.getDateOfBirth() != null) {
            user.setDateOfBirth(user.getDateOfBirth().toLocalDate().atStartOfDay());
        }
        userService.addUser(user);

        return "redirect:/";
    }

    private UserFormView toFormView(User user) {
        UserFormView userForm = modelMapper.map(user, UserFormView.class);
        if (user.getDateOfBirth() != null) {
            userForm.setDay(user.getDateOfBirth().getDayOfMonth());
            userForm.setMonth(ConvertHelper.monthToString(user.getDateOfBirth().getMonthValue()));
            userForm.setYear(user.getDateOfBirth().getYear());
        } else {
            userForm.setYear(-1);
        }
        return userForm;
    }
}

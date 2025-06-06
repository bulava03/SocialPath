package com.example.SocialPath.web;

import com.example.SocialPath.document.User;
import com.example.SocialPath.helper.ConvertHelper;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.ModelAttributesService;
import com.example.SocialPath.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.ui.Model;
import com.example.SocialPath.extraClasses.UserForm;
import com.example.SocialPath.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelAttributesService modelAttributesService;

    @GetMapping()
    public String getRegistrationPage(Model model) {
        UserForm user = new UserForm();
        user.setYear(-1);
        model.addAttribute("User", user);
        return "registration/registration";
    }

    @PostMapping("/registration")
    public String registration(User user, Model model) {
        if (userService.findByLogin(user.getLogin()) != null) {
            UserForm userForm = modelMapper.map(user, UserForm.class);
            userForm.setDay(user.getDateOfBirth().getDayOfMonth());
            userForm.setMonth(ConvertHelper.monthToString(user.getDateOfBirth().getMonthValue()));
            userForm.setYear(user.getDateOfBirth().getYear());

            model.addAttribute("User", userForm);
            model.addAttribute("errorText", "Цей логін вже зайнято!");
            return "registration/registration";
        }

        Object[] validation = userService.validateUser(user);
        if (!(boolean) validation[0]) {
            UserForm userForm = modelMapper.map(user, UserForm.class);
            userForm.setDay(user.getDateOfBirth().getDayOfMonth());
            userForm.setMonth(ConvertHelper.monthToString(user.getDateOfBirth().getMonthValue()));
            userForm.setYear(user.getDateOfBirth().getYear());

            model.addAttribute("User", userForm);
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "registration/registration";
        } else {
            user.setDateOfBirth(LocalDateTime.of(user.getDateOfBirth().getYear(), user.getDateOfBirth().getMonthValue(), user.getDateOfBirth().getDayOfMonth(), 0, 0, 0));
            user.setType(0);
            userService.addUser(user);

            return "redirect:/";
        }
    }

}

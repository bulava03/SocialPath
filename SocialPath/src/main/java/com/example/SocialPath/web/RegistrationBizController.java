package com.example.SocialPath.web;

import com.example.SocialPath.document.Biz;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.BizCreationForm;
import com.example.SocialPath.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/registration-biz")
public class RegistrationBizController {

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private UserService userService;
    @Autowired
    private BizService bizService;
    @Autowired
    private ModelMapper modelMapper;

    @GetMapping()
    public String getRegistrationPage(Model model) {
        BizCreationForm biz = new BizCreationForm();
        model.addAttribute("biz", biz);
        model.addAttribute("onlyOnline", false);
        return "registration/registration-biz";
    }

    @PostMapping("/registration-biz")
    public String registration(Biz biz, Model model) {
        BizCreationForm bizCreationForm = modelMapper.map(biz, BizCreationForm.class);

        if (userService.findByLogin(biz.getLogin()) != null) {
            model.addAttribute("biz", bizCreationForm);
            model.addAttribute("onlyOnline", biz.isOnlyOnline());
            model.addAttribute("errorText", "Цей логін вже зайнято!");
            return "registration/registration-biz";
        }

        Object[] validation = bizService.validateBiz(bizCreationForm);
        if (!(boolean) validation[0]) {
            model.addAttribute("biz", bizCreationForm);
            model.addAttribute("onlyOnline", biz.isOnlyOnline());
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "registration/registration-biz";
        } else {
            List<String> jobsUpdated = new ArrayList<>();

            for (String str : biz.getJobs()) {
                if (!str.isEmpty()) {
                    jobsUpdated.add(str);
                }
            }

            biz.setJobs(jobsUpdated);

            User user = modelMapper.map(biz, User.class);
            user.setType(1);
            userService.addUser(user);

            return "redirect:/";
        }
    }

}

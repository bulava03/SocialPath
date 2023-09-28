package com.example.SocialPath.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequestMapping()
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("errorText", "");
        return "home/index";
    }

}

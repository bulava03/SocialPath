package com.example.SocialPath_Admin.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/group")
public class GroupController {

    @GetMapping("/getGroupPage")
    public String getGroupPage() {
        return "group/groupPage";
    }

}

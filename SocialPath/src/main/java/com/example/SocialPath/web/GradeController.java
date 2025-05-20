package com.example.SocialPath.web;

import com.example.SocialPath.document.Grade;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.GradeService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/grades")
public class GradeController {

    @Autowired
    private GradeService gradeService;
    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/addGrade")
    public String addGrade(HttpServletRequest req, Grade grade, Model model) {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        grade.setUserLogin(login);
        Grade gradeFounded = gradeService.findByUserLoginAndBizLogin(login, grade.getBizLogin());
        if (gradeFounded != null) {
            grade.setId(gradeFounded.getId());
        }
        gradeService.save(grade);

        return "redirect:/user/anotherUserPage?anotherUserLogin=" + grade.getBizLogin();
    }

    @PostMapping("/deleteGrade")
    public String deleteGrade(HttpServletRequest req, Grade grade, Model model) {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        gradeService.deleteByUserLoginAndBizLogin(login, grade.getBizLogin());

        return "redirect:/user/anotherUserPage?anotherUserLogin=" + grade.getBizLogin();
    }

}

package com.example.SocialPath.web.rest;

import com.example.SocialPath.document.Biz;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.UserLogin;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.BizService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private UserService userService;
    private BizService bizService;
    private JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, UserService userService, BizService bizService, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.bizService = bizService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping
    @RequestMapping("/login")
    public String authorize(UserLogin request) {
        String login = request.getLogin();
        String password = request.getPassword();

        try {
            User user = userService.findByLogin(login);
            if (!user.getPassword().equals(password)) {
                throw (new Exception("Unknown user"));
            }
            return jwtTokenProvider.createToken(user.getLogin(), user.getPassword());
        } catch (Exception ex) {
            return "fail";
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
        securityContextLogoutHandler.logout(request, response, null);
    }

}

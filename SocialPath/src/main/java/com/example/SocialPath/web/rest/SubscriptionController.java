package com.example.SocialPath.web.rest;

import com.example.SocialPath.document.User;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscription")
public class SubscriptionController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/subscribe")
    public int subscribe(HttpServletRequest request, String login) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);

        if (myUser == null) {
            return -1;
        }

        User anotherUser = userService.findUserById(login);

        if (anotherUser == null) {
            return -1;
        }

        userService.subscribe(authorLogin, login);
        return 0;
    }

    @PostMapping("/unsubscribe")
    public int unsubscribe(HttpServletRequest request, String login) {
        String token = userService.resolveToken(request);
        if (token == null) {
            return -1;
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(authorLogin);

        if (myUser == null) {
            return -1;
        }

        User anotherUser = userService.findUserById(login);

        if (anotherUser == null) {
            return -1;
        }

        userService.unsubscribe(authorLogin, login);
        return 0;
    }

}

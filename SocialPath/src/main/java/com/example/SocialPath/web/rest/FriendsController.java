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
@RequestMapping("/friends")
public class FriendsController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/inviteToFriends")
    public int inviteToFriends(HttpServletRequest request, String login) {
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

         if (anotherUser.getFriends() != null && anotherUser.getFriends().contains(authorLogin)) {
             return 0;
         } else if (anotherUser.getFriendInvites() != null && anotherUser.getFriendInvites().contains(authorLogin)) {
             return 1;
         } else {
             userService.inviteUser(authorLogin, login);
             return 2;
         }
    }

    @PostMapping("/removeInviteToFriends")
    public int removeInviteToFriends(HttpServletRequest request, String login) {
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

        if (anotherUser.getFriends() != null && anotherUser.getFriends().contains(authorLogin)) {
            userService.removeFromFriends(login, authorLogin);
            return 0;
        } else if (anotherUser.getFriendInvites() != null && anotherUser.getFriendInvites().contains(authorLogin)) {
            userService.removeFromInvitations(login, authorLogin);
            return 1;
        }

        return -1;
    }

    @PostMapping("/rejectInvitationToFriends")
    public int rejectInvitationToFriends(HttpServletRequest request, String login) {
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

        userService.removeFromInvitations(authorLogin, login);

        return 0;
    }

    @PostMapping("/acceptToFriends")
    public int acceptToFriends(HttpServletRequest request, String login) {
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

        if (myUser.getFriends() != null && myUser.getFriends().contains(login)) {
            return 0;
        } else if (myUser.getFriendInvites() != null && myUser.getFriendInvites().contains(login)) {
            userService.acceptToFriends(authorLogin, login);
            userService.removeFromInvitations(authorLogin, login);
            return 1;
        }

        return -1;
    }

    @PostMapping("/removeFromFriends")
    public int removeFromFriends(HttpServletRequest request, String login) {
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

        userService.removeFromFriends(login, authorLogin);

        return 0;
    }

}

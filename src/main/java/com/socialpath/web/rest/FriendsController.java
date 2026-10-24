package com.socialpath.web.rest;

import com.socialpath.entity.User;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendsController {

    private final UserService userService;

    @PostMapping("/inviteToFriends")
    public OperationResult inviteToFriends(@CurrentUser User me, String login) {
        String authorLogin = me.getLogin();
        User anotherUser = requireUser(login);

        if (anotherUser.getFriends().contains(authorLogin)) {
            return OperationResult.of("ALREADY_FRIENDS");
        }
        if (anotherUser.getFriendInvites().contains(authorLogin)) {
            return OperationResult.of("ALREADY_INVITED");
        }
        userService.inviteUser(authorLogin, login);
        return OperationResult.of("INVITED");
    }

    @PostMapping("/removeInviteToFriends")
    public OperationResult removeInviteToFriends(@CurrentUser User me, String login) {
        String authorLogin = me.getLogin();
        User anotherUser = requireUser(login);

        if (anotherUser.getFriends().contains(authorLogin)) {
            userService.removeFromFriends(login, authorLogin);
            return OperationResult.of("REMOVED_FROM_FRIENDS");
        }
        if (anotherUser.getFriendInvites().contains(authorLogin)) {
            userService.removeFromInvitations(login, authorLogin);
            return OperationResult.of("INVITE_CANCELLED");
        }
        return OperationResult.of("NO_RELATION");
    }

    @PostMapping("/rejectInvitationToFriends")
    public OperationResult rejectInvitationToFriends(@CurrentUser User me, String login) {
        String authorLogin = me.getLogin();
        requireUser(login);

        userService.removeFromInvitations(authorLogin, login);
        return OperationResult.of("INVITE_REJECTED");
    }

    @PostMapping("/acceptToFriends")
    public OperationResult acceptToFriends(@CurrentUser User myUser, String login) {
        String authorLogin = myUser.getLogin();
        requireUser(login);

        if (myUser.getFriends().contains(login)) {
            return OperationResult.of("ALREADY_FRIENDS");
        }
        if (myUser.getFriendInvites().contains(login)) {
            userService.acceptToFriends(authorLogin, login);
            userService.removeFromInvitations(authorLogin, login);
            return OperationResult.of("ACCEPTED");
        }
        return OperationResult.of("NO_INVITE");
    }

    @PostMapping("/removeFromFriends")
    public OperationResult removeFromFriends(@CurrentUser User me, String login) {
        String authorLogin = me.getLogin();
        requireUser(login);

        userService.removeFromFriends(login, authorLogin);
        return OperationResult.of("REMOVED_FROM_FRIENDS");
    }

    private User requireUser(String login) {
        User user = userService.findByLogin(login);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + login);
        }
        return user;
    }
}

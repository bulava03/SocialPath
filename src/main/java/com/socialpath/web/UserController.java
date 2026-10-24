package com.socialpath.web;

import com.socialpath.entity.User;
import com.socialpath.dto.request.ChangePasswordForm;
import com.socialpath.dto.request.FoundedUser;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.dto.response.FriendshipStatus;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.exception.InvalidImageException;
import com.socialpath.validation.ValidationResult;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.CommentsService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CommentsService commentsService;
    private final ModelAttributesService modelAttributesService;
    private final HandleAvatarService handleAvatarService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/authorisation")
    public String getUserPageFirst(@CurrentUser User user, Model model) {
        model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadUserFeed(user.getLogin()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("myUser", user);

        return "user/userPage";
    }

    @GetMapping("/anotherUserPage")
    public String getAnotherUserPage(@CurrentUser User myUser, FoundedUser foundedUser, Model model) {
        String login = myUser.getLogin();

        if (foundedUser.getAnotherUserLogin().equals(login)) {
            return "redirect:/user/authorisation";
        }

        User user = userService.findByLogin(foundedUser.getAnotherUserLogin());

        if (user == null) {
            return "redirect:/user/authorisation";
        }

        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("myUser", myUser);
        model.addAttribute("author", new UserLogin(login, ""));
        model.addAttribute("isAuthor", user.getLogin().equals(login));
        model.addAttribute("friendship", FriendshipStatus.between(myUser, user));
        model.addAttribute("publications", commentsService.loadUserFeed(user.getLogin()));
        model = handleAvatarService.handleAvatar(myUser, model, false);
        model = handleAvatarService.handleAvatar(user, model, true);

        return "user/userPage";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(@CurrentUser User user, Model model) {
        model = modelAttributesService.userInfoModel(model, user, user.getLogin());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/userInfo";
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfo(@CurrentUser User myUser, UserUpdate user, MultipartFile file, Model model) throws IOException {
        ValidationResult validation = userService.validateUserUpdate(user);
        if (!validation.isSuccess()) {
            return showUserInfoWithError(model, user, myUser, validation.getMessage());
        }

        try {
            user = handleAvatarService.updateAvatar(user, myUser, file);
        } catch (InvalidImageException e) {
            return showUserInfoWithError(model, user, myUser, e.getMessage());
        }
        user.setLogin(myUser.getLogin());

        userService.updateUser(user);

        return "redirect:/user/authorisation";
    }

    /**
     * Shows the profile form again with the submitted values and an error.
     * The current avatar is kept in the model so the header and the avatar
     * picker still show it.
     * @param model the model to populate
     * @param user the submitted profile update
     * @param myUser the persisted user
     * @param errorText the message to show
     * @return the profile form view
     */
    private String showUserInfoWithError(Model model, UserUpdate user, User myUser, String errorText) {
        model = modelAttributesService.userInfoModel(model, user, myUser.getLogin());
        model = handleAvatarService.handleAvatar(myUser, model, false);
        model.addAttribute("errorText", errorText);
        return "user/userInfo";
    }

    @GetMapping("/getUsersGroups")
    public String getUsersGroups(@CurrentUser User user, LeftFrameRequest request, Model model) {
        List<GroupSearchResult> list = userService.findUsersGroups(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("groups", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersGroups";
    }

    @GetMapping("/getUsersFriends")
    public String getUsersFriends(@CurrentUser User user, LeftFrameRequest request, Model model) {
        List<UserSearchResult> list = userService.findUsersFriends(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriends";
    }

    @GetMapping("/getInvitationsToFriends")
    public String getInvitationsToFriends(@CurrentUser User user, LeftFrameRequest request, Model model) {
        List<UserSearchResult> list = userService.findUsersFriendsInvitations(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriendsInvitations";
    }

    @PostMapping("/acceptToFriendsMyPage")
    public String acceptToFriendsMyPage(@CurrentUser User myUser, LeftFrameRequest request) {
        String login = myUser.getLogin();

        // Only accept if a real pending invitation exists, so a forged request
        // can't fabricate a friendship out of nothing.
        if (myUser.getFriendInvites().contains(request.getId())) {
            userService.acceptToFriends(login, request.getId());
            userService.removeFromInvitations(login, request.getId());
        }

        return "redirect:/user/getInvitationsToFriends?id=" + login;
    }

    @PostMapping("/rejectInvitationToFriendsMyPage")
    public String rejectInvitationToFriendsMyPage(@CurrentUser User me, LeftFrameRequest request) {
        String login = me.getLogin();

        userService.removeFromInvitations(login, request.getId());

        return "redirect:/user/getInvitationsToFriends?id=" + login;
    }

    @PostMapping("/removeFromFriendsMyPage")
    public String removeFromFriendsMyPage(@CurrentUser User me, LeftFrameRequest request) {
        String login = me.getLogin();

        userService.removeFromFriends(login, request.getId());

        return "redirect:/user/getUsersFriends?id=" + login;
    }

    @GetMapping("/getChangePasswordForm")
    public String getChangePasswordForm(@CurrentUser User myUser, Model model) {
        model = handleAvatarService.handleAvatar(myUser, model, false);
        model.addAttribute("errorText", "");

        return "user/changePasswordForm";
    }

    @PostMapping("/changePassword")
    public String changePassword(@CurrentUser User myUser, @Valid ChangePasswordForm form, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model = handleAvatarService.handleAvatar(myUser, model, false);
            model.addAttribute("errorText", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "user/changePasswordForm";
        }

        if (!passwordEncoder.matches(form.getOldPassword(), myUser.getPassword())) {
            model = handleAvatarService.handleAvatar(myUser, model, false);
            model.addAttribute("errorText", "The old password is incorrect.");
            return "user/changePasswordForm";
        }

        userService.updatePassword(myUser.getLogin(), form.getPassword());

        return "redirect:/user/authorisation";
    }
}

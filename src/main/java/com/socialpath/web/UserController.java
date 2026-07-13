package com.socialpath.web;

import com.socialpath.document.User;
import com.socialpath.dto.request.ChangePasswordForm;
import com.socialpath.dto.request.FoundedUser;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserLogin;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.helper.CheckHelper;
import com.socialpath.validation.ValidationResult;
import com.socialpath.security.SecurityUtils;
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
    public String getUserPageFirst(Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("myUser", user);

        return "user/userPage";
    }

    @GetMapping("/anotherUserPage")
    public String getAnotherUserPage(FoundedUser foundedUser, Model model) throws IOException {
        String login = SecurityUtils.getCurrentLogin();

        if (foundedUser.getAnotherUserLogin().equals(login)) {
            return "redirect:/user/authorisation";
        }

        User myUser = userService.findByLogin(login);
        User user = userService.findByLogin(foundedUser.getAnotherUserLogin());

        if (user == null) {
            return "redirect:/user/authorisation";
        }

        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("myUser", myUser);
        model.addAttribute("author", new UserLogin(login, ""));
        model.addAttribute("isAuthor", user.getLogin().equals(login));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, login, foundedUser.getAnotherUserLogin()));
        model.addAttribute("publications", commentsService.loadComments("User", foundedUser.getAnotherUserLogin()));
        model = handleAvatarService.handleAvatar(myUser, model, false);
        model = handleAvatarService.handleAvatar(user, model, true);

        return "user/userPage";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        model = modelAttributesService.userInfoModel(model, user, user.getLogin());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/userInfo";
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfo(UserUpdate user, MultipartFile file, Model model) throws IOException {
        String login = SecurityUtils.getCurrentLogin();

        ValidationResult validation = userService.validateUserUpdate(user);
        if (!validation.isSuccess()) {
            model = modelAttributesService.userInfoModel(model, user, login);
            model.addAttribute("errorText", validation.getMessage());
            return "user/userInfo";
        }

        User myUser = userService.findByLogin(login);

        if (user.getDateOfBirth() != null) {
            user.setDateOfBirth(user.getDateOfBirth().toLocalDate().atStartOfDay());
        }
        user = handleAvatarService.updateAvatar(user, myUser, file);
        user.setLogin(myUser.getLogin());

        userService.updateUser(user);

        return "redirect:/user/authorisation";
    }

    @GetMapping("/getUsersGroups")
    public String getUsersGroups(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        List<GroupSearchResult> list = userService.findUsersGroups(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("groups", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersGroups";
    }

    @GetMapping("/getUsersFriends")
    public String getUsersFriends(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        List<UserSearchResult> list = userService.findUsersFriends(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriends";
    }

    @GetMapping("/getInvitationsToFriends")
    public String getInvitationsToFriends(LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriendsInvitations";
    }

    @PostMapping("/acceptToFriendsMyPage")
    public String acceptToFriendsMyPage(LeftFrameRequest request) {
        String login = SecurityUtils.getCurrentLogin();
        User myUser = userService.findByLogin(login);

        // Only accept if a real pending invitation exists, so a forged request
        // can't fabricate a friendship out of nothing.
        if (myUser.getFriendInvites() != null && myUser.getFriendInvites().contains(request.getId())) {
            userService.acceptToFriends(login, request.getId());
            userService.removeFromInvitations(login, request.getId());
        }

        return "redirect:/user/getInvitationsToFriends?id=" + login;
    }

    @PostMapping("/rejectInvitationToFriendsMyPage")
    public String rejectInvitationToFriendsMyPage(LeftFrameRequest request) {
        String login = SecurityUtils.getCurrentLogin();

        userService.removeFromInvitations(login, request.getId());

        return "redirect:/user/getInvitationsToFriends?id=" + login;
    }

    @PostMapping("/removeFromFriendsMyPage")
    public String removeFromFriendsMyPage(LeftFrameRequest request) {
        String login = SecurityUtils.getCurrentLogin();

        userService.removeFromFriends(login, request.getId());

        return "redirect:/user/getUsersFriends?id=" + login;
    }

    @GetMapping("/getChangePasswordForm")
    public String getChangePasswordForm(Model model) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());

        model = handleAvatarService.handleAvatar(myUser, model, false);
        model.addAttribute("errorText", "");

        return "user/changePasswordForm";
    }

    @PostMapping("/changePassword")
    public String changePassword(@Valid ChangePasswordForm form, BindingResult bindingResult, Model model) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());

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

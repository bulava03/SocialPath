package com.example.SocialPath.web;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.helper.ConvertHelper;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.ModelAttributesService;
import com.example.SocialPath.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelAttributesService modelAttributesService;

    @GetMapping("/authorisation")
    public String getUserPageFirst(@ModelAttribute("user") User user, Model model) {
        user = userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
        return "user/userPage";
    }

    @GetMapping("/anotherUserPage")
    public String getAnotherUserPage(@ModelAttribute("user") FoundedUser foundedUser, Model model) {
        User myUser = userService.findUserByLoginAndPassword(foundedUser.getLogin(), foundedUser.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (userService.findUserById(foundedUser.getAnotherUserLogin()) == null) {
            model = modelAttributesService.usersAttributes(model, myUser, true, commentsService.loadComments("User", foundedUser.getLogin()));
            return "user/userPage";
        }

        User user = userService.findUserById(foundedUser.getAnotherUserLogin());
        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(foundedUser.getLogin(), foundedUser.getPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(foundedUser.getLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, foundedUser.getLogin(), foundedUser.getAnotherUserLogin()));
        model.addAttribute("publications", commentsService.loadComments("User", foundedUser.getAnotherUserLogin()));
        return "user/userPage";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(@ModelAttribute("user") User user, Model model) {
        user = userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        model = modelAttributesService.userInfoModel(model, user, new UserLogin(user.getLogin(), user.getPassword()));
        return "user/userInfo";
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfo(@ModelAttribute("user") UserUpdate user, Model model) {
        Object[] validation = userService.validateUserUpdate(user);
        if (!(boolean) validation[0]) {
            model = modelAttributesService.userInfoModel(model, user, new UserLogin(user.getLogin(), user.getPasswordOld()));
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "user/userInfo";
        } else {
            User myUser = userService.findUserByLoginAndPassword(user.getLogin(), user.getPasswordOld());

            if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
            }

            user.setDateOfBirth(LocalDateTime.of(user.getDateOfBirth().getYear(), user.getDateOfBirth().getMonthValue(), user.getDateOfBirth().getDayOfMonth(), 0, 0, 0));

            userService.updateUser(user);

            model = modelAttributesService.usersAttributes(model, userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword()), true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }
    }

    @GetMapping("/getUsersGroups")
    public String getUsersGroups(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<GroupSearchResult> list = userService.findUsersGroups(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("groups", list);
        model.addAttribute("login", request.getId());
        return "user/usersGroups";
    }

    @GetMapping("/getUsersFriends")
    public String getUsersFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriends(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        return "user/usersFriends";
    }

    @GetMapping("/getInvitationsToFriends")
    public String getInvitationsToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        return "user/usersFriendsInvitations";
    }

    @PostMapping("/inviteToFriends")
    public String inviteToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        User user = userService.findUserById(request.getId());

        if (user.getFriendInvites() != null) {
            if (user.getFriendInvites().contains(request.getAuthorLogin()))
            {
                userService.acceptToFriends(request.getAuthorLogin(), request.getId());
                userService.removeFromInvitations(request.getAuthorLogin(), request.getId());
            } else {
                userService.inviteUser(request.getAuthorLogin(), request.getId());
            }
        } else {
            userService.inviteUser(request.getAuthorLogin(), request.getId());
        }

        user = userService.findUserById(request.getId());
        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(request.getAuthorLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, request.getAuthorLogin(), request.getId()));
        model.addAttribute("publications", commentsService.loadComments("User", request.getId()));
        return "user/userPage";
    }

    @PostMapping("/removeInviteToFriends")
    public String removeInviteToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        userService.removeFromInvitations(request.getId(), request.getAuthorLogin());

        User user = userService.findUserById(request.getId());

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(request.getAuthorLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, request.getAuthorLogin(), request.getId()));
        model.addAttribute("publications", commentsService.loadComments("User", request.getId()));
        return "user/userPage";
    }

    @PostMapping("/acceptToFriends")
    public String acceptToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        if (myUser.getFriendInvites().contains(request.getId())) {
            userService.acceptToFriends(request.getAuthorLogin(), request.getId());
            //userService.acceptToFriends(request.getId(), request.getAuthorLogin());
            userService.removeFromInvitations(request.getAuthorLogin(), request.getId());
        }

        myUser = userService.findUserById(request.getAuthorLogin());
        User user = userService.findUserById(request.getId());

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(request.getAuthorLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, request.getAuthorLogin(), request.getId()));
        model.addAttribute("publications", commentsService.loadComments("User", request.getId()));
        return "user/userPage";
    }

    @PostMapping("/rejectInvitationToFriends")
    public String rejectInvitationToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        userService.removeFromInvitations(request.getAuthorLogin(), request.getId());

        myUser = userService.findUserById(request.getAuthorLogin());
        User user = userService.findUserById(request.getId());

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(request.getAuthorLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, request.getAuthorLogin(), request.getId()));
        model.addAttribute("publications", commentsService.loadComments("User", request.getId()));
        return "user/userPage";
    }

    @PostMapping("/removeFromFriends")
    public String removeFromFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        userService.removeFromFriends(request.getAuthorLogin(), request.getId());
        //userService.removeFromFriends(request.getId(), request.getAuthorLogin());

        myUser = userService.findUserById(request.getAuthorLogin());
        User user = userService.findUserById(request.getId());

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(request.getAuthorLogin(), request.getAuthorPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(request.getAuthorLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, request.getAuthorLogin(), request.getId()));
        model.addAttribute("publications", commentsService.loadComments("User", request.getId()));
        return "user/userPage";
    }

    @PostMapping("/acceptToFriendsMyPage")
    public String acceptToFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        if (myUser.getFriendInvites().contains(request.getId())) {
            userService.acceptToFriends(request.getAuthorLogin(), request.getId());
            //userService.acceptToFriends(request.getId(), request.getAuthorLogin());
            userService.removeFromInvitations(request.getAuthorLogin(), request.getId());
        }

        myUser = userService.findUserById(request.getAuthorLogin());

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(myUser.getLogin());

        model.addAttribute("author", myUser);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getAuthorLogin());
        return "user/usersFriendsInvitations";
    }

    @PostMapping("/rejectInvitationToFriendsMyPage")
    public String rejectInvitationToFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        userService.removeFromInvitations(request.getAuthorLogin(), request.getId());

        myUser = userService.findUserById(request.getAuthorLogin());

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(myUser.getLogin());

        model.addAttribute("author", myUser);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getAuthorLogin());
        return "user/usersFriendsInvitations";
    }

    @PostMapping("/removeFromFriendsMyPage")
    public String removeFromFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
        }

        userService.removeFromFriends(request.getAuthorLogin(), request.getId());
        //userService.removeFromFriends(request.getId(), request.getAuthorLogin());

        myUser = userService.findUserById(request.getAuthorLogin());

        List<UserSearchResult> list = userService.findUsersFriends(myUser.getLogin());

        model.addAttribute("author", myUser);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getAuthorLogin());
        return "user/usersFriends";
    }

}

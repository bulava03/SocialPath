package com.example.SocialPath.web;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.FileStorageService;
import com.example.SocialPath.service.ModelAttributesService;
import com.example.SocialPath.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
    @Autowired
    private FileStorageService fileStorageService;

    private static final long MAX_AVATAR_SIZE = 50 * 1024 * 1024; // 50 MB

    @GetMapping("/authorisation")
    public String getUserPageFirst(@ModelAttribute("user") User user, Model model) throws IOException {
        user = userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        return "user/userPage";
    }

    @GetMapping("/anotherUserPage")
    public String getAnotherUserPage(@ModelAttribute("user") FoundedUser foundedUser, Model model) throws IOException {
        if (foundedUser.getAnotherUserLogin().equals(foundedUser.getLogin())) {
            return "redirect:/user/authorisation?login=" + foundedUser.getLogin() + "&password=" + foundedUser.getPassword();
        }

        User myUser = userService.findUserByLoginAndPassword(foundedUser.getLogin(), foundedUser.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        } else if (userService.findUserById(foundedUser.getAnotherUserLogin()) == null) {
            return "redirect:/user/authorisation?login=" + foundedUser.getLogin() + "&password=" + foundedUser.getPassword();
        }

        User user = userService.findUserById(foundedUser.getAnotherUserLogin());
        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(foundedUser.getLogin(), foundedUser.getPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(foundedUser.getLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, foundedUser.getLogin(), foundedUser.getAnotherUserLogin()));
        model.addAttribute("publications", commentsService.loadComments("User", foundedUser.getAnotherUserLogin()));

        if (myUser.getImageId() != null && !myUser.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(myUser.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatarAnother", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatarAnother", null);
        }

        return "user/userPage";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(@ModelAttribute("user") User user, Model model) throws IOException {
        user = userService.findUserByLoginAndPassword(user.getLogin(), user.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        model = modelAttributesService.userInfoModel(model, user, new UserLogin(user.getLogin(), user.getPassword()));

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        return "user/userInfo";
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfo(UserUpdate user, MultipartFile file, Model model) throws IOException {
        Object[] validation = userService.validateUserUpdate(user);
        if (!(boolean) validation[0]) {
            model = modelAttributesService.userInfoModel(model, user, new UserLogin(user.getLogin(), user.getPasswordOld()));
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "user/userInfo";
        } else {
            User myUser = userService.findUserByLoginAndPassword(user.getLogin(), user.getPasswordOld());

            if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
            }

            user.setDateOfBirth(LocalDateTime.of(user.getDateOfBirth().getYear(), user.getDateOfBirth().getMonthValue(), user.getDateOfBirth().getDayOfMonth(), 0, 0, 0));

            if (!file.isEmpty()) {
                if (!Objects.equals(myUser.getImageId(), "") && (myUser.getImageId() != null)) {
                    fileStorageService.deleteFileById(myUser.getImageId());
                    user.setImageId("");
                }
                String fileId = fileStorageService.storeFile(file);
                user.setImageId(fileId);
            } else {
                if (!Objects.equals(user.getImageId(), "") && (user.getImageId() != null)) {
                    fileStorageService.deleteFileById(myUser.getImageId());
                    user.setImageId("");
                } else {
                    user.setImageId("");
                }
            }

            userService.updateUser(user);

            return "redirect:/user/authorisation?login=" + user.getLogin() + "&password=" + user.getPassword();
        }
    }

    @GetMapping("/getUsersGroups")
    public String getUsersGroups(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<GroupSearchResult> list = userService.findUsersGroups(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("groups", list);
        model.addAttribute("login", request.getId());

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        return "user/usersGroups";
    }

    @GetMapping("/getUsersFriends")
    public String getUsersFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriends(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        return "user/usersFriends";
    }

    @GetMapping("/getInvitationsToFriends")
    public String getInvitationsToFriends(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        return "user/usersFriendsInvitations";
    }

    @PostMapping("/acceptToFriendsMyPage")
    public String acceptToFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        if (myUser.getFriendInvites().contains(request.getId())) {
            userService.acceptToFriends(request.getAuthorLogin(), request.getId());
            userService.removeFromInvitations(request.getAuthorLogin(), request.getId());
        }

        myUser = userService.findUserById(request.getAuthorLogin());

        return "redirect:/user/getInvitationsToFriends?id=" + myUser.getLogin() +
                "&authorLogin=" + myUser.getLogin() + "&authorPassword=" + myUser.getPassword();
    }

    @PostMapping("/rejectInvitationToFriendsMyPage")
    public String rejectInvitationToFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        userService.removeFromInvitations(request.getAuthorLogin(), request.getId());

        myUser = userService.findUserById(request.getAuthorLogin());

        return "redirect:/user/getInvitationsToFriends?id=" + myUser.getLogin() +
                "&authorLogin=" + myUser.getLogin() + "&authorPassword=" + myUser.getPassword();
    }

    @PostMapping("/removeFromFriendsMyPage")
    public String removeFromFriendsMyPage(@ModelAttribute("request") LeftFrameRequest request, Model model) throws IOException {
        User myUser = userService.findUserByLoginAndPassword(request.getAuthorLogin(), request.getAuthorPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        userService.removeFromFriends(request.getAuthorLogin(), request.getId());

        myUser = userService.findUserById(request.getAuthorLogin());

        return "redirect:/user/getUsersFriends?id=" + myUser.getLogin() +
                "&authorLogin=" + myUser.getLogin() + "&authorPassword=" + myUser.getPassword();
    }

}

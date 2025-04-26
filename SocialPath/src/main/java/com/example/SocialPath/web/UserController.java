package com.example.SocialPath.web;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private BizService bizService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ModelAttributesService modelAttributesService;
    @Autowired
    private HandleAvatarService handleAvatarService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private static final long MAX_AVATAR_SIZE = 50 * 1024 * 1024; // 50 MB

    @GetMapping("/authorisation")
    public String getUserPageFirst(HttpServletRequest request, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("myUser", user);

        return "user/userPage";
    }

    @GetMapping("/anotherUserPage")
    public String getAnotherUserPage(HttpServletRequest request, FoundedUser foundedUser, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        if (foundedUser.getAnotherUserLogin().equals(login)) {
            return "redirect:/user/authorisation?login=" + foundedUser.getLogin() + "&password=" + foundedUser.getPassword();
        }

        User myUser = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        if (userService.findUserById(foundedUser.getAnotherUserLogin()) == null) {
            return "redirect:/user/authorisation?login=" + foundedUser.getLogin() + "&password=" + foundedUser.getPassword();
        }

        User user = userService.findUserById(foundedUser.getAnotherUserLogin());
        user.setPassword("");

        boolean isSubscribed;
        if (myUser.getSubscriptions() == null) {
            isSubscribed = false;
        } else {
            isSubscribed = myUser.getSubscriptions().contains(myUser.getLogin());
        }

        model.addAttribute("user", user);
        model.addAttribute("myUser", myUser);
        model.addAttribute("isSubscribed", isSubscribed);
        model.addAttribute("author", new UserLogin(foundedUser.getLogin(), foundedUser.getPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(foundedUser.getLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, foundedUser.getLogin(), foundedUser.getAnotherUserLogin()));
        model.addAttribute("publications", commentsService.loadComments("User", foundedUser.getAnotherUserLogin()));
        model = handleAvatarService.handleAvatar(myUser, model, false);
        model = handleAvatarService.handleAvatar(user, model, true);

        return "user/userPage";
    }

    @GetMapping("/getUserInfo")
    public String getUserInfo(HttpServletRequest request, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        if (user.getType() == 0) {
            model = modelAttributesService.userInfoModel(model, user, new UserLogin(user.getLogin(), user.getPassword()));
            model.addAttribute("concreteAddress", user.getConcreteAddress());
            model.addAttribute("longitude", user.getLongitude());
            model.addAttribute("latitude", user.getLatitude());
            model.addAttribute("onlyOnline", user.isOnlyOnline());
            model = handleAvatarService.handleAvatar(user, model, false);

            return "user/userInfo";
        } else {
            model.addAttribute("user", user);
            model.addAttribute("author", new UserLogin(user.getLogin(), user.getPassword()));
            model.addAttribute("concreteAddress", user.getConcreteAddress());
            model.addAttribute("longitude", user.getLongitude());
            model.addAttribute("latitude", user.getLatitude());
            model.addAttribute("onlyOnline", user.isOnlyOnline());
            model = handleAvatarService.handleAvatar(user, model, false);

            return "user/bizInfo";
        }
    }

    @PostMapping("/changeUserInfo")
    public String changeUserInfo(HttpServletRequest request, UserUpdate user, MultipartFile file, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        user.setName("111");
        user.setSlogan("111");

        Object[] validation = userService.validateUserUpdate(user);
        if (!(boolean) validation[0]) {
            model = modelAttributesService.userInfoModel(model, user, new UserLogin(login, ""));
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "user/userInfo";
        } else {
            User myUser = userService.findByLogin(login);

            if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
            }

            user.setDateOfBirth(LocalDateTime.of(user.getDateOfBirth().getYear(), user.getDateOfBirth().getMonthValue(), user.getDateOfBirth().getDayOfMonth(), 0, 0, 0));
            user = handleAvatarService.updateAvatar(user, myUser, file);
            user.setLogin(myUser.getLogin());
            user.setName("");
            user.setSlogan("");

            userService.updateUser(user);

            return "redirect:/user/authorisation";
        }
    }

    @PostMapping("/changeBizInfo")
    public String changeBizInfo(HttpServletRequest request, BizUpdate biz, MultipartFile file, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        BizCreationForm bizCreationForm = modelMapper.map(biz, BizCreationForm.class);
        bizCreationForm.setLogin(login);

        Object[] validation = bizService.validateBiz(bizCreationForm);
        if (!(boolean) validation[0]) {
            model.addAttribute("user", modelMapper.map(biz, UserFormUpdate.class));
            model.addAttribute("author", new UserLogin(login, ""));
            model.addAttribute("errorText", validation[1].toString().replaceAll("Optional\\[|\\]", ""));
            return "user/bizInfo";
        } else {
            UserUpdate user = modelMapper.map(biz, UserUpdate.class);

            User myUser = userService.findByLogin(login);

            if (!CheckHelper.nullOrBannedCheck(myUser).isEmpty()) {
                model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
                return "home/index";
            }

            user = handleAvatarService.updateAvatar(user, myUser, file);
            user.setLogin(myUser.getLogin());

            userService.updateBiz(user);

            return "redirect:/user/authorisation";
        }
    }

    @GetMapping("/getUsersGroups")
    public String getUsersGroups(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<GroupSearchResult> list = userService.findUsersGroups(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("groups", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersGroups";
    }

    @GetMapping("/getUsersFriends")
    public String getUsersFriends(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriends(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriends";
    }

    @GetMapping("/getInvitationsToFriends")
    public String getInvitationsToFriends(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(login);

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        List<UserSearchResult> list = userService.findUsersFriendsInvitations(request.getId());

        model.addAttribute("author", user);
        model.addAttribute("users", list);
        model.addAttribute("login", request.getId());
        model = handleAvatarService.handleAvatar(user, model, false);

        return "user/usersFriendsInvitations";
    }

    @PostMapping("/acceptToFriendsMyPage")
    public String acceptToFriendsMyPage(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

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
    public String rejectInvitationToFriendsMyPage(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

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
    public String removeFromFriendsMyPage(HttpServletRequest req, LeftFrameRequest request, Model model) throws IOException {
        String token = userService.resolveToken(req);
        if (token == null) {
            return "redirect:/";
        }
        String login = jwtTokenProvider.getUsernameFromToken(token);

        User myUser = userService.findByLogin(login);

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

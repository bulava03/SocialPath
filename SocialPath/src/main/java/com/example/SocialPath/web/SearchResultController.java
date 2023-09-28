package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.FoundedGroup;
import com.example.SocialPath.extraClasses.FoundedUser;
import com.example.SocialPath.extraClasses.SearchRequest;
import com.example.SocialPath.extraClasses.UserLogin;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.service.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/searchResult")
public class SearchResultController {
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private ModelAttributesService modelAttributesService;

    @GetMapping("/getSearchResult")
    public String getSearchResult(@ModelAttribute("request") SearchRequest request, Model model) {
        User user = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        Object[] searchResult = searchService.SearchUsersAndGroups(request.getSearchText(), request.getLogin());

        model.addAttribute("author", user);
        model.addAttribute("users", searchResult[0]);
        model.addAttribute("groups", searchResult[1]);
        return "searchResult/searchResult";
    }

    @GetMapping("/getUserPage")
    public String GetUserPage(@ModelAttribute("foundedUser") FoundedUser foundedUser, Model model) {
        User myUser = userService.findUserByLoginAndPassword(foundedUser.getLogin(), foundedUser.getPassword());

        if (!CheckHelper.nullOrBannedCheck(myUser).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(myUser));
            return "home/index";
        }

        if (userService.findUserById(foundedUser.getAnotherUserLogin()) == null) {
            User user = userService.findUserById(foundedUser.getLogin());
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        User user = userService.findUserById(foundedUser.getAnotherUserLogin());
        user.setPassword("");

        myUser = userService.findUserById(foundedUser.getLogin());

        model.addAttribute("user", user);
        model.addAttribute("author", new UserLogin(foundedUser.getLogin(), foundedUser.getPassword()));
        model.addAttribute("isAuthor", user.getLogin().equals(foundedUser.getLogin()));
        model.addAttribute("InRequests", CheckHelper.inRequestsCheck(user, myUser, foundedUser.getLogin(), foundedUser.getAnotherUserLogin()));
        model.addAttribute("publications", commentsService.loadComments("User", foundedUser.getAnotherUserLogin()));
        return "user/userPage";
    }

    @GetMapping("/getGroupPage")
    public String getGroupPage(@ModelAttribute("foundedGroup") FoundedGroup foundedGroup, Model model) {
        User user = userService.findUserByLoginAndPassword(foundedGroup.getLogin(), foundedGroup.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).equals("")) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        if (groupService.findGroupById(new ObjectId(foundedGroup.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        Group group = groupService.findGroupById(new ObjectId(foundedGroup.getGroupId()));
        model.addAttribute("group", group);
        model.addAttribute("author", new UserLogin(user.getLogin(), user.getPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", foundedGroup.getGroupId().toString()));
        return "group/groupPage";
    }

}

package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.service.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

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
    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/getSearchResult")
    public String getSearchResult(@ModelAttribute("request") SearchRequest request, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(request.getLogin(), request.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
            model.addAttribute("errorText", CheckHelper.nullOrBannedCheck(user));
            return "home/index";
        }

        Object[] searchResult = searchService.searchUsersAndGroups(request.getSearchText(), request.getLogin());

        model.addAttribute("author", user);
        model.addAttribute("users", searchResult[0]);
        model.addAttribute("groups", searchResult[1]);
        return "searchResult/searchResult";
    }

    @GetMapping("/getUserPage")
    public String getUserPage(@ModelAttribute("foundedUser") FoundedUser foundedUser, Model model) {
        return "redirect:/user/anotherUserPage?login=" + foundedUser.getLogin() +
                "&password=" + foundedUser.getPassword() +
                "&anotherUserLogin=" + foundedUser.getAnotherUserLogin();
    }

    @GetMapping("/getGroupPage")
    public String getGroupPage(@ModelAttribute("foundedGroup") FoundedGroup foundedGroup, Model model) throws IOException {
        User user = userService.findUserByLoginAndPassword(foundedGroup.getLogin(), foundedGroup.getPassword());

        if (!CheckHelper.nullOrBannedCheck(user).isEmpty()) {
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
        model.addAttribute("publications", commentsService.loadComments("Group", foundedGroup.getGroupId()));

        if (user.getImageId() != null && !user.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(user.getImageId());
            model.addAttribute("avatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("avatar", null);
        }

        if (group.getImageId() != null && !group.getImageId().isEmpty()) {
            GridFsResource file = fileStorageService.getFileById(group.getImageId());
            model.addAttribute("groupAvatar", fileStorageService.convertGridFsFileToBase64(file));
        } else {
            model.addAttribute("groupAvatar", null);
        }

        return "group/groupPage";
    }

}

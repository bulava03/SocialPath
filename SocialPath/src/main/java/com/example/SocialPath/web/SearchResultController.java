package com.example.SocialPath.web;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.helper.CheckHelper;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.HandleAvatarService;
import com.example.SocialPath.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
    private HandleAvatarService handleAvatarService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/getSearchResult")
    public String getSearchResult(HttpServletRequest req, SearchRequest request, Model model) throws IOException {
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

        Object[] searchResult = searchService.searchUsersAndGroupsAndBizes(request.getSearchText(), login);

        model.addAttribute("user", user);
        model = handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("users", searchResult[0]);
        model.addAttribute("groups", searchResult[1]);
        model.addAttribute("bizes", searchResult[2]);

        return "searchResult/searchResult";
    }

    @GetMapping("/getUserPage")
    public String getUserPage(FoundedUser foundedUser, Model model) {
        return "redirect:/user/anotherUserPage?anotherUserLogin=" + foundedUser.getAnotherUserLogin();
    }

    @GetMapping("/getGroupPage")
    public String getGroupPage(HttpServletRequest req, FoundedGroup foundedGroup, Model model) throws IOException {
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

        if (groupService.findGroupById(new ObjectId(foundedGroup.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        Group group = groupService.findGroupById(new ObjectId(foundedGroup.getGroupId()));

        model.addAttribute("group", group);
        model.addAttribute("author", new UserLogin(user.getLogin(), user.getPassword()));
        model.addAttribute("publications", commentsService.loadComments("Group", foundedGroup.getGroupId()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model = handleAvatarService.handleAvatar(group, model);

        return "group/groupPage";
    }

}

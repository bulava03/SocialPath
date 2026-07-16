package com.socialpath.web;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/searchResult")
@RequiredArgsConstructor
public class SearchResultController {
    private final UserService userService;
    private final GroupService groupService;
    private final CommentsService commentsService;
    private final SearchService searchService;
    private final ModelAttributesService modelAttributesService;
    private final HandleAvatarService handleAvatarService;

    @GetMapping("/getSearchResult")
    public String getSearchResult(SearchRequest request, Model model) throws IOException {
        String login = SecurityUtils.getCurrentLogin();
        User user = userService.findByLogin(login);

        SearchResults searchResult = searchService.searchUsersAndGroups(request.getSearchText(), login);

        model.addAttribute("user", user);
        model = handleAvatarService.handleAvatar(user, model, false);
        model.addAttribute("users", searchResult.users());
        model.addAttribute("groups", searchResult.groups());

        return "searchResult/searchResult";
    }

    @GetMapping("/getUserPage")
    public String getUserPage(FoundedUser foundedUser, Model model) {
        return "redirect:/user/anotherUserPage?anotherUserLogin=" + foundedUser.getAnotherUserLogin();
    }

    @GetMapping("/getGroupPage")
    public String getGroupPage(FoundedGroup foundedGroup, Model model) throws IOException {
        User user = userService.findByLogin(SecurityUtils.getCurrentLogin());

        if (groupService.findGroupById(Long.valueOf(foundedGroup.getGroupId())) == null) {
            model = modelAttributesService.usersAttributes(model, user, true, commentsService.loadComments("User", user.getLogin()));
            return "user/userPage";
        }

        Group group = groupService.findGroupById(Long.valueOf(foundedGroup.getGroupId()));

        model.addAttribute("group", group);
        model.addAttribute("author", new UserLogin(user.getLogin(), ""));
        model.addAttribute("publications", commentsService.loadComments("Group", foundedGroup.getGroupId()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model = handleAvatarService.handleAvatar(group, model);

        return "group/groupPage";
    }
}

package com.socialpath.web;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/searchResult")
@RequiredArgsConstructor
public class SearchResultController {
    private final GroupService groupService;
    private final CommentsService commentsService;
    private final SearchService searchService;
    private final HandleAvatarService handleAvatarService;

    @GetMapping("/getSearchResult")
    public String getSearchResult(@CurrentUser User user, SearchRequest request, Model model) {
        String login = user.getLogin();

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
    public String getGroupPage(@CurrentUser User user, FoundedGroup foundedGroup, Model model) {
        Group group = groupService.getGroup(foundedGroup.getGroupId());

        model.addAttribute("group", group);
        model.addAttribute("author", new UserLogin(user.getLogin(), ""));
        model.addAttribute("publications", commentsService.loadGroupFeed(group.getId()));
        model = handleAvatarService.handleAvatar(user, model, false);
        model = handleAvatarService.handleAvatar(group, model);

        return "group/groupPage";
    }
}

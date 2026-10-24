package com.socialpath.web;

import com.socialpath.entity.Group;
import lombok.RequiredArgsConstructor;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/comments/view")
@RequiredArgsConstructor
public class CommentsViewController {
    private final UserService userService;
    private final GroupService groupService;
    private final CommentsService commentsService;
    private final ModelAttributesService modelAttributesService;

    @GetMapping("/loadCommentsUser")
    public String loadCommentsUser(String login, Model model) {
        String authorLogin = SecurityUtils.getCurrentLogin();

        List<PublicationPresentable> publications = commentsService.loadUserFeed(login);

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("user", userService.findByLogin(login));

        return "user/partsOfPages/rightFrame :: publications";
    }

    @GetMapping("/loadCommentsGroup")
    public String loadCommentsGroup(String groupId, Model model) {
        String authorLogin = SecurityUtils.getCurrentLogin();

        Group group = groupService.getGroup(groupId);
        List<PublicationPresentable> publications = commentsService.loadGroupFeed(group.getId());

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("group", group);

        return "group/partsOfPages/rightFrame :: publications";
    }
}

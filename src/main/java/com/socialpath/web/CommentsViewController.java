package com.socialpath.web;

import lombok.RequiredArgsConstructor;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
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
    public String loadCommentsUser(String login, Model model) throws IOException {
        String authorLogin = SecurityUtils.getCurrentLogin();

        List<PublicationPresentable> publications = commentsService.loadComments(commentsService.getCommentsIdsUser(login));

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("user", userService.findByLogin(login));

        return "user/partsOfPages/rightFrame :: publications";
    }

    @GetMapping("/loadCommentsGroup")
    public String loadCommentsGroup(String groupId, Model model) throws IOException {
        String authorLogin = SecurityUtils.getCurrentLogin();

        List<PublicationPresentable> publications = commentsService.loadComments(commentsService.getCommentsIdsGroup(new ObjectId(groupId)));

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("group", groupService.findGroupById(new ObjectId(groupId)));

        return "group/partsOfPages/rightFrame :: publications";
    }
}

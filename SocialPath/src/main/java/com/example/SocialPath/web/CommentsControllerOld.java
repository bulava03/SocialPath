package com.example.SocialPath.web;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.*;
import com.example.SocialPath.security.JwtTokenProvider;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.ModelAttributesService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/commentsOld")
public class CommentsControllerOld {
    @Autowired
    private UserService userService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private CommentsService commentsService;
    @Autowired
    private ModelAttributesService modelAttributesService;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/loadCommentsUser")
    public String loadCommentsUser(HttpServletRequest request, String login, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "";
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(authorLogin);
        if (user == null) {
            return "";
        }

        List<PublicationPresentable> publications = commentsService.loadComments(commentsService.getCommentsIdsUser(login));

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("user", userService.findByLogin(login));

        return "user/partsOfPages/rightFrame :: publications";
    }

    @GetMapping("/loadCommentsGroup")
    public String loadCommentsGroup(HttpServletRequest request, String groupId, Model model) throws IOException {
        String token = userService.resolveToken(request);
        if (token == null) {
            return "";
        }
        String authorLogin = jwtTokenProvider.getUsernameFromToken(token);

        User user = userService.findByLogin(authorLogin);
        if (user == null) {
            return "";
        }

        List<PublicationPresentable> publications = commentsService.loadComments(commentsService.getCommentsIdsGroup(new ObjectId(groupId)));

        model.addAttribute("publications", publications);
        model.addAttribute("author", new UserLogin(authorLogin, ""));
        model.addAttribute("group", groupService.findGroupById(new ObjectId(groupId)));

        return "/group/partsOfPages/rightFrame :: publications";
    }

}

package com.socialpath.web;

import com.socialpath.entity.Group;
import lombok.RequiredArgsConstructor;
import com.socialpath.dto.request.*;
import com.socialpath.dto.response.*;
import com.socialpath.entity.Publication;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.entity.User;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public String loadCommentsUser(@CurrentUser User me, String login, Long before, Model model) {
        FeedPage feed = commentsService.loadUserFeed(login, before);

        model.addAttribute("publications", feed.posts());
        model.addAttribute("nextBefore", feed.nextBefore());
        model.addAttribute("author", new UserLogin(me.getLogin(), ""));
        model.addAttribute("user", userService.findByLogin(login));

        return "user/partsOfPages/rightFrame :: posts";
    }

    @GetMapping("/loadCommentsGroup")
    public String loadCommentsGroup(@CurrentUser User me, String groupId, Long before, Model model) {
        Group group = groupService.getGroup(groupId);
        FeedPage feed = commentsService.loadGroupFeed(group.getId(), before);

        model.addAttribute("publications", feed.posts());
        model.addAttribute("nextBefore", feed.nextBefore());
        model.addAttribute("author", new UserLogin(me.getLogin(), ""));
        model.addAttribute("group", group);

        return "group/partsOfPages/rightFrame :: posts";
    }

    /**
     * Newest activity by others in a user page's feed; the open page polls
     * it to notice new posts and comments.
     * @param me the signed-in user
     * @param login the page owner
     * @return the largest publication id by someone else, 0 when none
     */
    @GetMapping("/latestUser")
    @ResponseBody
    public LatestActivity latestUser(@CurrentUser User me, String login) {
        return new LatestActivity(commentsService.latestUserFeedActivity(login, me.getLogin()));
    }

    /**
     * Newest activity by others in a group's feed.
     * @param me the signed-in user
     * @param groupId the group id
     * @return the largest publication id by someone else, 0 when none
     */
    @GetMapping("/latestGroup")
    @ResponseBody
    public LatestActivity latestGroup(@CurrentUser User me, String groupId) {
        Group group = groupService.getGroup(groupId);
        return new LatestActivity(commentsService.latestGroupFeedActivity(group.getId(), me.getLogin()));
    }

    /**
     * Renders one post with its whole comment tree, so the page can refresh
     * it in place after a comment or a deletion.
     * @param me the signed-in user
     * @param id the post or any comment under it
     * @param model the model to populate
     * @return the post rendered as on its user or group page
     */
    @GetMapping("/thread")
    public String loadThread(@CurrentUser User me, Long id, Model model) {
        Publication publication = id == null ? null : commentsService.findById(id);
        if (publication == null) {
            throw new ResourceNotFoundException("Publication not found: " + id);
        }
        PublicationPresentable thread = commentsService.loadThread(publication);
        Publication root = commentsService.findById(thread.getId());

        model.addAttribute("publications", List.of(thread));
        model.addAttribute("nextBefore", null);
        model.addAttribute("author", new UserLogin(me.getLogin(), ""));
        if (root.getGroupId() != null) {
            model.addAttribute("group", groupService.getGroup(root.getGroupId().toString()));
            return "group/partsOfPages/rightFrame :: posts";
        }
        model.addAttribute("user", userService.findByLogin(root.getAuthorId()));
        return "user/partsOfPages/rightFrame :: posts";
    }
}

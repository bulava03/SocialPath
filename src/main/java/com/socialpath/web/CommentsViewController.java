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
        model.addAttribute("author", me);
        model.addAttribute("user", userService.findByLogin(login));

        return "user/partsOfPages/rightFrame :: posts";
    }

    @GetMapping("/loadCommentsGroup")
    public String loadCommentsGroup(@CurrentUser User me, String groupId, Long before, Model model) {
        Group group = groupService.getGroup(groupId);
        FeedPage feed = commentsService.loadGroupFeed(group.getId(), before);

        model.addAttribute("publications", feed.posts());
        model.addAttribute("nextBefore", feed.nextBefore());
        model.addAttribute("author", me);
        model.addAttribute("group", group);

        return "group/partsOfPages/rightFrame :: posts";
    }

    /**
     * Newest activity by others in a user page's feed; the open page polls
     * it to notice new posts.
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
     * Renders one post or comment, its replies only counted, so the page can
     * show a new one or refresh one in place.
     * @param me the signed-in user
     * @param id the post or comment
     * @param model the model to populate
     * @return the publication rendered as on its user or group page
     */
    @GetMapping("/publication")
    public String loadPublication(@CurrentUser User me, Long id, Model model) {
        Publication publication = requirePublication(id);

        model.addAttribute("publications", List.of(commentsService.loadPublication(publication)));
        model.addAttribute("nextBefore", null);
        model.addAttribute("author", me);
        return placeOf(publication, model) + "/partsOfPages/rightFrame :: posts";
    }

    /**
     * Renders a portion of the direct replies of a post or comment, with a
     * button for the rest when there is more.
     * @param me the signed-in user
     * @param parentId the post or comment
     * @param before continue below this reply id; null for the newest replies
     * @param model the model to populate
     * @return the replies rendered as on their user or group page
     */
    @GetMapping("/replies")
    public String loadReplies(@CurrentUser User me, Long parentId, Long before, Model model) {
        Publication parent = requirePublication(parentId);
        RepliesPage page = commentsService.loadReplies(parent.getId(), before);

        model.addAttribute("replies", page.replies());
        model.addAttribute("parentId", parent.getId());
        model.addAttribute("hidden", page.hidden());
        model.addAttribute("next", page.next());
        model.addAttribute("before", page.nextBefore());
        model.addAttribute("author", me);
        return placeOf(parent, model).equals("group")
                ? "renderComment/renderCommentGroup :: replies"
                : "renderComment/renderCommentUser :: replies";
    }

    private Publication requirePublication(Long id) {
        Publication publication = id == null ? null : commentsService.findById(id);
        if (publication == null) {
            throw new ResourceNotFoundException("Publication not found: " + id);
        }
        return publication;
    }

    /**
     * Puts the page a publication lives on into the model: its group, or the
     * user whose page it is (the author of the thread's top-level post).
     * @param publication a post or a comment at any depth
     * @param model the model to populate
     * @return "group" or "user", the templates' folder
     */
    private String placeOf(Publication publication, Model model) {
        Publication root = commentsService.findRoot(publication);
        if (root.getGroupId() != null) {
            model.addAttribute("group", groupService.getGroup(root.getGroupId().toString()));
            return "group";
        }
        model.addAttribute("user", userService.findByLogin(root.getAuthorId()));
        return "user";
    }
}

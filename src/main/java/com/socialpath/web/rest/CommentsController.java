package com.socialpath.web.rest;

import com.socialpath.entity.Group;
import com.socialpath.entity.Publication;
import com.socialpath.entity.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Publishing and deleting posts and comments on user pages and group pages.
 * Every permission check is based on stored data: where a comment lives is
 * read from the top-level publication of its thread (see
 * {@link CommentsService#findRoot}), never from ids the client sends along.
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final GroupService groupService;
    private final CommentsService commentsService;

    @PostMapping("/addUserPublication")
    public OperationResult addUserPublication(@CurrentUser User myUser, NewPublication publication) throws IOException {
        requireContent(publication.getText(), publication.getMedia());

        publication.setAuthorId(myUser.getLogin());
        return OperationResult.created(commentsService.addNewUserPublication(publication));
    }

    @PostMapping("/addCommentUser")
    public OperationResult addCommentUser(@CurrentUser User myUser, NewComment newComment) throws IOException {
        Publication root = commentsService.findRoot(requirePublication(newComment.getIdPublication()));
        // Group threads are commented through the group endpoint, which checks membership.
        if (root.getGroupId() != null) {
            throw new ForbiddenOperationException("Not a user page publication");
        }
        requireContent(newComment.getText(), newComment.getMedia());

        newComment.setAuthorLogin(myUser.getLogin());
        commentsService.addNewComment(newComment);
        return OperationResult.of("OK");
    }

    @PostMapping("/removeCommentUser")
    public OperationResult removeCommentUser(@CurrentUser User me, DelComment delComment) {
        String login = me.getLogin();
        Publication target = requirePublication(delComment.getIdComment());
        Publication root = commentsService.findRoot(target);
        if (root.getGroupId() != null) {
            throw new ForbiddenOperationException("Not a user page publication");
        }

        // The root's author owns the page: they may delete anything in their
        // threads, everyone else only their own posts and comments.
        if (!isAuthor(target, login) && !isAuthor(root, login)) {
            throw new ForbiddenOperationException("Not allowed to delete this comment");
        }
        commentsService.deletePublication(target.getId());
        return OperationResult.of("OK");
    }

    @PostMapping("/addGroupPublication")
    public OperationResult addGroupPublication(@CurrentUser User myUser, NewPublication publication) throws IOException {
        requireMember(groupService.getGroup(publication.getGroupId()), myUser.getLogin());
        requireContent(publication.getText(), publication.getMedia());

        publication.setAuthorId(myUser.getLogin());
        return OperationResult.created(commentsService.addNewGroupPublication(publication));
    }

    @PostMapping("/addCommentGroup")
    public OperationResult addCommentGroup(@CurrentUser User myUser, NewComment newComment) throws IOException {
        Group group = groupService.getGroup(newComment.getGroupId());
        requireMember(group, myUser.getLogin());
        requireInGroup(requirePublication(newComment.getIdPublication()), group);
        requireContent(newComment.getText(), newComment.getMedia());

        newComment.setAuthorLogin(myUser.getLogin());
        commentsService.addNewComment(newComment);
        return OperationResult.of("OK");
    }

    @PostMapping("/removeCommentGroup")
    public OperationResult removeCommentGroup(@CurrentUser User me, DelComment delComment) {
        String login = me.getLogin();
        Group group = groupService.getGroup(delComment.getGroupId());
        Publication target = requirePublication(delComment.getIdComment());
        requireInGroup(target, group);

        // In groups, deletion is allowed for the author, the owner and admins.
        if (!isAuthor(target, login) && !group.isModerator(login)) {
            throw new ForbiddenOperationException("Not allowed to delete this comment");
        }
        commentsService.deletePublication(target.getId());
        return OperationResult.of("OK");
    }

    private Publication requirePublication(Long id) {
        Publication publication = id == null ? null : commentsService.findById(id);
        if (publication == null) {
            throw new IllegalArgumentException("Empty content or missing target");
        }
        return publication;
    }

    /**
     * Makes sure a publication or comment belongs to the given group's page.
     * @param publication a publication or a comment at any depth
     * @param group the group named in the request
     */
    private void requireInGroup(Publication publication, Group group) {
        if (!group.getId().equals(commentsService.findRoot(publication).getGroupId())) {
            throw new ForbiddenOperationException("The publication is not in this group");
        }
    }

    private void requireMember(Group group, String login) {
        if (!group.isMember(login)) {
            throw new ForbiddenOperationException("Only group members can publish here");
        }
    }

    private boolean isAuthor(Publication publication, String login) {
        return publication.getAuthorId() != null && publication.getAuthorId().equals(login);
    }

    private void requireContent(String text, List<MultipartFile> media) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasMedia = media != null && !media.isEmpty();
        if (!hasText && !hasMedia) {
            throw new IllegalArgumentException("Empty content or missing target");
        }
    }
}

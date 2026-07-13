package com.socialpath.web.rest;

import com.socialpath.document.Group;
import com.socialpath.document.Publication;
import com.socialpath.document.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    /** Sentinel value the front-end sends when the target is a top-level publication. */
    private static final String TOP_LEVEL_PUBLICATION = "publications";

    private final UserService userService;
    private final GroupService groupService;
    private final CommentsService commentsService;

    @PostMapping("/addUserPublication")
    public OperationResult addUserPublication(NewPublication publication) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());

        if (hasContent(publication)) {
            publication.setAuthorId(myUser.getLogin());
            commentsService.addNewUserPublication(publication);
            return OperationResult.of("OK");
        }

        throw new IllegalArgumentException("Empty content or missing target");
    }

    @PostMapping("/addCommentUser")
    public OperationResult addCommentUser(NewComment newComment) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());

        if (commentsService.findById(newComment.getIdPublication()) != null && hasContent(newComment)) {
            newComment.setAuthorLogin(myUser.getLogin());
            commentsService.addNewComment(newComment);
            return OperationResult.of("OK");
        }

        throw new IllegalArgumentException("Empty content or missing target");
    }

    @PostMapping("/removeCommentUser")
    public OperationResult removeCommentUser(DelComment delComment) {
        String login = SecurityUtils.getCurrentLogin();

        Publication target = commentsService.findById(delComment.getIdComment());
        if (target == null) {
            throw new IllegalArgumentException("Empty content or missing target");
        }

        if (TOP_LEVEL_PUBLICATION.equals(delComment.getIdPublication())) {
            // A publication on a user page always belongs to the page owner.
            requireAuthor(target, login);
            delComment.setLogin(target.getAuthorId());
            commentsService.removePublicationUser(delComment);
            return OperationResult.of("OK");
        }

        Publication parent = commentsService.findById(new ObjectId(delComment.getIdPublication()));
        if (parent == null) {
            throw new IllegalArgumentException("Empty content or missing target");
        }

        // Deleting a comment is allowed for its author and for the page owner.
        if (!isAuthor(target, login) && !isAuthor(parent, login)) {
            throw new ForbiddenOperationException("Not allowed to delete this comment");
        }
        commentsService.removeComment(delComment);
        return OperationResult.of("OK");
    }

    @PostMapping("/addGroupPublication")
    public OperationResult addGroupPublication(NewPublication publication) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());
        requireGroup(publication.getGroupId());

        if (hasContent(publication)) {
            publication.setAuthorId(myUser.getLogin());
            commentsService.addNewGroupPublication(publication);
            return OperationResult.of("OK");
        }

        throw new IllegalArgumentException("Empty content or missing target");
    }

    @PostMapping("/addCommentGroup")
    public OperationResult addCommentGroup(NewComment newComment) throws IOException {
        User myUser = userService.findByLogin(SecurityUtils.getCurrentLogin());
        requireGroup(newComment.getGroupId());

        if (commentsService.findById(newComment.getIdPublication()) != null && hasContent(newComment)) {
            newComment.setAuthorLogin(myUser.getLogin());
            commentsService.addNewComment(newComment);
            return OperationResult.of("OK");
        }

        throw new IllegalArgumentException("Empty content or missing target");
    }

    @PostMapping("/removeCommentGroup")
    public OperationResult removeCommentGroup(DelComment delComment) {
        String login = SecurityUtils.getCurrentLogin();
        Group group = requireGroup(delComment.getGroupId());

        Publication target = commentsService.findById(delComment.getIdComment());
        if (target == null) {
            throw new IllegalArgumentException("Empty content or missing target");
        }

        // In groups, deletion is allowed for the author, the owner and admins.
        boolean isModerator = (group.getOwner() != null && group.getOwner().equals(login))
                || (group.getAdmins() != null && group.getAdmins().contains(login));
        if (!isAuthor(target, login) && !isModerator) {
            throw new ForbiddenOperationException("Not allowed to delete this comment");
        }

        if (TOP_LEVEL_PUBLICATION.equals(delComment.getIdPublication())) {
            delComment.setLogin(login);
            commentsService.removePublicationGroup(delComment);
            return OperationResult.of("OK");
        }

        if (commentsService.findById(new ObjectId(delComment.getIdPublication())) == null) {
            throw new ResourceNotFoundException("Publication not found: " + delComment.getIdPublication());
        }
        commentsService.removeComment(delComment);
        return OperationResult.of("OK");
    }

    private Group requireGroup(String groupId) {
        Group group = groupService.findGroupById(new ObjectId(groupId));
        if (group == null) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }
        return group;
    }

    private boolean isAuthor(Publication publication, String login) {
        return publication.getAuthorId() != null && publication.getAuthorId().equals(login);
    }

    private void requireAuthor(Publication publication, String login) {
        if (!isAuthor(publication, login)) {
            throw new ForbiddenOperationException("Not allowed to delete this publication");
        }
    }

    private boolean hasContent(NewPublication publication) {
        return (publication.getText() != null && !publication.getText().trim().isEmpty())
                || (publication.getMedia() != null && !publication.getMedia().isEmpty());
    }

    private boolean hasContent(NewComment comment) {
        return (comment.getText() != null && !comment.getText().trim().isEmpty())
                || (comment.getMedia() != null && !comment.getMedia().isEmpty());
    }
}

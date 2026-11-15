package com.socialpath.web.rest;

import com.socialpath.entity.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.security.CurrentUser;
import com.socialpath.service.CommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Publishing and deleting posts and comments on user pages and group pages.
 * The author is always the signed-in user; who may do what is decided by
 * {@link CommentsService}.
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;

    @PostMapping("/addUserPublication")
    public OperationResult addUserPublication(@CurrentUser User me, NewPublication publication) throws IOException {
        return OperationResult.created(commentsService.publishOnUserPage(me.getLogin(), publication));
    }

    @PostMapping("/addCommentUser")
    public OperationResult addCommentUser(@CurrentUser User me, NewComment comment) throws IOException {
        return OperationResult.created(commentsService.commentOnUserPage(me.getLogin(), comment));
    }

    @PostMapping("/removeCommentUser")
    public OperationResult removeCommentUser(@CurrentUser User me, DelComment delComment) {
        commentsService.deleteFromUserPage(me.getLogin(), delComment.getIdComment());
        return OperationResult.of("OK");
    }

    @PostMapping("/addGroupPublication")
    public OperationResult addGroupPublication(@CurrentUser User me, NewPublication publication) throws IOException {
        return OperationResult.created(commentsService.publishInGroup(me.getLogin(), publication));
    }

    @PostMapping("/addCommentGroup")
    public OperationResult addCommentGroup(@CurrentUser User me, NewComment comment) throws IOException {
        return OperationResult.created(commentsService.commentInGroup(me.getLogin(), comment));
    }

    @PostMapping("/removeCommentGroup")
    public OperationResult removeCommentGroup(@CurrentUser User me, DelComment delComment) {
        commentsService.deleteFromGroup(me.getLogin(), delComment.getGroupId(), delComment.getIdComment());
        return OperationResult.of("OK");
    }
}

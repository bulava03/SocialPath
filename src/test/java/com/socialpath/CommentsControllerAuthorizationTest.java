package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.entity.Publication;
import com.socialpath.dto.request.DelComment;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.service.UserService;
import com.socialpath.web.rest.CommentsController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentsControllerAuthorizationTest {

    private CommentsService commentsService;
    private GroupService groupService;
    private CommentsController controller;

    private final Long publicationId = 10L;
    private final Long commentId = 11L;
    private final Long groupId = 12L;

    @BeforeEach
    void setUp() {
        commentsService = mock(CommentsService.class);
        groupService = mock(GroupService.class);
        controller = new CommentsController(mock(UserService.class), groupService, commentsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    private Publication publication(Long id, String author) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setAuthorId(author);
        return publication;
    }

    private DelComment topLevelDelete() {
        DelComment delComment = new DelComment();
        delComment.setIdPublication("publications");
        delComment.setIdComment(publicationId);
        return delComment;
    }

    private DelComment nestedDelete() {
        DelComment delComment = new DelComment();
        delComment.setIdPublication(publicationId.toString());
        delComment.setIdComment(commentId);
        return delComment;
    }

    @Test
    void removeCommentUser_AuthorDeletesOwnPublication_Succeeds() {
        loginAs("alice");
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "alice"));

        controller.removeCommentUser(topLevelDelete());

        verify(commentsService).removePublicationUser(any(DelComment.class));
    }

    @Test
    void removeCommentUser_StrangerCannotDeletePublication() {
        loginAs("mallory");
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "alice"));

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentUser(topLevelDelete()));
        verify(commentsService, never()).removePublicationUser(any());
    }

    @Test
    void removeCommentUser_CommentAuthorDeletesOwnComment_Succeeds() {
        loginAs("bob");
        when(commentsService.findById(commentId)).thenReturn(publication(commentId, "bob"));
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "alice"));

        controller.removeCommentUser(nestedDelete());

        verify(commentsService).removeComment(any(DelComment.class));
    }

    @Test
    void removeCommentUser_PageOwnerDeletesForeignComment_Succeeds() {
        loginAs("alice");
        when(commentsService.findById(commentId)).thenReturn(publication(commentId, "bob"));
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "alice"));

        controller.removeCommentUser(nestedDelete());

        verify(commentsService).removeComment(any(DelComment.class));
    }

    @Test
    void removeCommentUser_StrangerCannotDeleteForeignComment() {
        loginAs("mallory");
        when(commentsService.findById(commentId)).thenReturn(publication(commentId, "bob"));
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "alice"));

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentUser(nestedDelete()));
        verify(commentsService, never()).removeComment(any());
    }

    @Test
    void removeCommentGroup_AdminDeletesForeignPublication_Succeeds() {
        loginAs("admin");
        Group group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(new ArrayList<>(List.of("owner", "admin")));
        when(groupService.findGroupById(groupId)).thenReturn(group);
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "bob"));

        DelComment delComment = topLevelDelete();
        delComment.setGroupId(groupId.toString());

        controller.removeCommentGroup(delComment);

        verify(commentsService).removePublicationGroup(any(DelComment.class));
    }

    @Test
    void removeCommentGroup_StrangerCannotDeleteForeignPublication() {
        loginAs("mallory");
        Group group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(new ArrayList<>(List.of("owner", "admin")));
        when(groupService.findGroupById(groupId)).thenReturn(group);
        when(commentsService.findById(publicationId)).thenReturn(publication(publicationId, "bob"));

        DelComment delComment = topLevelDelete();
        delComment.setGroupId(groupId.toString());

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentGroup(delComment));
        verify(commentsService, never()).removePublicationGroup(any());
    }
}

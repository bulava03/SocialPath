package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.entity.Publication;
import com.socialpath.entity.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.service.CommentsService;
import com.socialpath.service.GroupService;
import com.socialpath.web.rest.CommentsController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentsControllerAuthorizationTest {

    private CommentsService commentsService;
    private CommentsController controller;

    // alice's page: her publication and bob's comment under it
    private final Long publicationId = 10L;
    private final Long commentId = 11L;
    // group 12: a publication by bob; group 99: a publication by victim
    private final Long groupId = 12L;
    private final Long groupPublicationId = 20L;
    private final Long otherGroupPublicationId = 30L;

    private final Map<Long, Publication> stored = new HashMap<>();

    @BeforeEach
    void setUp() {
        commentsService = mock(CommentsService.class);
        GroupService groupService = mock(GroupService.class);
        controller = new CommentsController(groupService, commentsService);

        store(publication(publicationId, "alice", null, null));
        store(publication(commentId, "bob", publicationId, null));
        store(publication(groupPublicationId, "bob", null, groupId));
        store(publication(otherGroupPublicationId, "victim", null, 99L));
        when(commentsService.findById(any())).thenAnswer(inv -> stored.get(inv.<Long>getArgument(0)));
        when(commentsService.findRoot(any())).thenAnswer(inv -> rootOf(inv.getArgument(0)));

        Group group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(List.of("owner", "admin"));
        group.setMembers(List.of("owner", "admin", "bob"));
        when(groupService.getGroup(groupId.toString())).thenReturn(group);
    }

    private User user(String login) {
        User user = new User();
        user.setLogin(login);
        return user;
    }

    private Publication publication(Long id, String author, Long parentId, Long inGroup) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setAuthorId(author);
        publication.setParentId(parentId);
        publication.setGroupId(inGroup);
        return publication;
    }

    private void store(Publication publication) {
        stored.put(publication.getId(), publication);
    }

    private Publication rootOf(Publication publication) {
        Publication current = publication;
        while (current.getParentId() != null) {
            current = stored.get(current.getParentId());
        }
        return current;
    }

    private DelComment delete(Long id) {
        DelComment delComment = new DelComment();
        delComment.setIdComment(id);
        return delComment;
    }

    private DelComment groupDelete(Long id) {
        DelComment delComment = delete(id);
        delComment.setGroupId(groupId.toString());
        return delComment;
    }

    private NewComment comment(Long parentId) {
        NewComment comment = new NewComment();
        comment.setIdPublication(parentId);
        comment.setText("hi");
        return comment;
    }

    private NewComment groupComment(Long parentId) {
        NewComment comment = comment(parentId);
        comment.setGroupId(groupId.toString());
        return comment;
    }

    private NewPublication groupPublication() {
        NewPublication publication = new NewPublication();
        publication.setGroupId(groupId.toString());
        publication.setText("hi");
        return publication;
    }

    @Test
    void removeCommentUser_AuthorDeletesOwnPublication_Succeeds() {
        controller.removeCommentUser(user("alice"), delete(publicationId));

        verify(commentsService).deletePublication(publicationId);
    }

    @Test
    void removeCommentUser_StrangerCannotDeletePublication() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentUser(user("mallory"), delete(publicationId)));
        verify(commentsService, never()).deletePublication(any());
    }

    @Test
    void removeCommentUser_CommentAuthorDeletesOwnComment_Succeeds() {
        controller.removeCommentUser(user("bob"), delete(commentId));

        verify(commentsService).deletePublication(commentId);
    }

    @Test
    void removeCommentUser_PageOwnerDeletesForeignComment_Succeeds() {
        controller.removeCommentUser(user("alice"), delete(commentId));

        verify(commentsService).deletePublication(commentId);
    }

    @Test
    void removeCommentUser_StrangerCannotDeleteForeignComment() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentUser(user("mallory"), delete(commentId)));
        verify(commentsService, never()).deletePublication(any());
    }

    @Test
    void removeCommentUser_CannotReachGroupPublications() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentUser(user("mallory"), delete(groupPublicationId)));
        verify(commentsService, never()).deletePublication(any());
    }

    @Test
    void removeCommentGroup_AdminDeletesForeignPublication_Succeeds() {
        controller.removeCommentGroup(user("admin"), groupDelete(groupPublicationId));

        verify(commentsService).deletePublication(groupPublicationId);
    }

    @Test
    void removeCommentGroup_StrangerCannotDeleteForeignPublication() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentGroup(user("mallory"), groupDelete(groupPublicationId)));
        verify(commentsService, never()).deletePublication(any());
    }

    @Test
    void removeCommentGroup_OwnerCannotDeleteOutsideTheirGroup() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentGroup(user("owner"), groupDelete(otherGroupPublicationId)));
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeCommentGroup(user("owner"), groupDelete(commentId)));
        verify(commentsService, never()).deletePublication(any());
    }

    @Test
    void addGroupPublication_Member_Succeeds() throws Exception {
        controller.addGroupPublication(user("bob"), groupPublication());

        verify(commentsService).addNewGroupPublication(any());
    }

    @Test
    void addGroupPublication_NonMemberIsForbidden() throws Exception {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addGroupPublication(user("mallory"), groupPublication()));
        verify(commentsService, never()).addNewGroupPublication(any());
    }

    @Test
    void addCommentGroup_NonMemberIsForbidden() throws Exception {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addCommentGroup(user("mallory"), groupComment(groupPublicationId)));
        verify(commentsService, never()).addNewComment(any());
    }

    @Test
    void addCommentGroup_PublicationOfAnotherGroupIsForbidden() throws Exception {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addCommentGroup(user("bob"), groupComment(otherGroupPublicationId)));
        verify(commentsService, never()).addNewComment(any());
    }

    @Test
    void addCommentUser_OnGroupPublicationIsForbidden() throws Exception {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addCommentUser(user("mallory"), comment(groupPublicationId)));
        verify(commentsService, never()).addNewComment(any());
    }

    @Test
    void addCommentUser_OnUserPagePublication_Succeeds() throws Exception {
        controller.addCommentUser(user("mallory"), comment(commentId));

        verify(commentsService).addNewComment(any());
    }
}

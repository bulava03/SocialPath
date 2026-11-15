package com.socialpath;

import com.socialpath.config.FeedProperties;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.entity.Group;
import com.socialpath.entity.Publication;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ThreadPlace;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Who may publish, comment and delete where. Every decision is made on stored
 * data: where a comment lives is read from the post at the top of its thread,
 * never from ids the client sends along.
 */
class CommentsPermissionTest {

    private CommentsRepository commentsRepository;
    private CommentsServiceImpl service;

    // alex's page: his post and sam's comment under it
    private final Long postId = 10L;
    private final Long commentId = 11L;
    // group 12: a post by sam; group 99: a post by a stranger
    private final Long groupId = 12L;
    private final Long groupPostId = 20L;
    private final Long otherGroupPostId = 30L;

    private final Map<Long, Publication> stored = new HashMap<>();

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        GroupService groupService = mock(GroupService.class);
        service = new CommentsServiceImpl(mock(UserRepository.class), commentsRepository,
                mock(FileStorageService.class), mock(VideoService.class), new FeedProperties(20, 10, 1),
                groupService);

        store(publication(postId, "alex", null, null));
        store(publication(commentId, "sam", postId, null));
        store(publication(groupPostId, "sam", null, groupId));
        store(publication(otherGroupPostId, "stranger", null, 99L));
        when(commentsRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(stored.get(inv.<Long>getArgument(0))));
        when(commentsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Group group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(List.of("owner", "admin"));
        group.setMembers(List.of("owner", "admin", "sam"));
        when(groupService.getGroup(groupId.toString())).thenReturn(group);
    }

    private Publication publication(Long id, String author, Long parentId, Long inGroup) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setAuthorId(author);
        publication.setParentId(parentId);
        publication.setRootId(parentId == null ? null : stored.get(parentId).getRootId() != null
                ? stored.get(parentId).getRootId() : parentId);
        publication.setGroupId(inGroup);
        return publication;
    }

    private void store(Publication publication) {
        stored.put(publication.getId(), publication);
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

    private NewPublication post(String text) {
        NewPublication publication = new NewPublication();
        publication.setText(text);
        return publication;
    }

    private NewPublication groupPost() {
        NewPublication publication = post("hi");
        publication.setGroupId(groupId.toString());
        return publication;
    }

    // ---------- deleting on user pages ----------

    @Test
    void deleteFromUserPage_AuthorDeletesOwnPost() {
        service.deleteFromUserPage("alex", postId);

        verify(commentsRepository).deleteThread(postId);
    }

    @Test
    void deleteFromUserPage_StrangerCannotDeleteAPost() {
        assertThrows(ForbiddenOperationException.class, () -> service.deleteFromUserPage("mallory", postId));
        verify(commentsRepository, never()).deleteThread(any());
    }

    @Test
    void deleteFromUserPage_CommentAuthorDeletesOwnComment() {
        service.deleteFromUserPage("sam", commentId);

        verify(commentsRepository).deleteThread(commentId);
    }

    @Test
    void deleteFromUserPage_PageOwnerDeletesSomeoneElsesComment() {
        service.deleteFromUserPage("alex", commentId);

        verify(commentsRepository).deleteThread(commentId);
    }

    @Test
    void deleteFromUserPage_StrangerCannotDeleteSomeoneElsesComment() {
        assertThrows(ForbiddenOperationException.class, () -> service.deleteFromUserPage("mallory", commentId));
        verify(commentsRepository, never()).deleteThread(any());
    }

    @Test
    void deleteFromUserPage_CannotReachGroupPosts() {
        assertThrows(ForbiddenOperationException.class, () -> service.deleteFromUserPage("sam", groupPostId));
        verify(commentsRepository, never()).deleteThread(any());
    }

    // ---------- deleting in groups ----------

    @Test
    void deleteFromGroup_AdminDeletesSomeoneElsesPost() {
        service.deleteFromGroup("admin", groupId.toString(), groupPostId);

        verify(commentsRepository).deleteThread(groupPostId);
    }

    @Test
    void deleteFromGroup_StrangerCannotDeleteSomeoneElsesPost() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.deleteFromGroup("mallory", groupId.toString(), groupPostId));
        verify(commentsRepository, never()).deleteThread(any());
    }

    @Test
    void deleteFromGroup_OwnerCannotDeleteOutsideTheirGroup() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.deleteFromGroup("owner", groupId.toString(), otherGroupPostId));
        assertThrows(ForbiddenOperationException.class,
                () -> service.deleteFromGroup("owner", groupId.toString(), commentId));
        verify(commentsRepository, never()).deleteThread(any());
    }

    // ---------- publishing and commenting ----------

    @Test
    void publishOnUserPage_TheAuthorIsTheSignedInUser() throws Exception {
        service.publishOnUserPage("alex", post("hello"));

        verify(commentsRepository).save(argThat(saved ->
                "alex".equals(saved.getAuthorId()) && saved.getGroupId() == null && saved.getParentId() == null));
    }

    @Test
    void publishOnUserPage_WithNothingToPublish_IsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.publishOnUserPage("alex", post(" ")));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void publishInGroup_Member_Succeeds() throws Exception {
        service.publishInGroup("sam", groupPost());

        verify(commentsRepository).save(argThat(saved ->
                groupId.equals(saved.getGroupId()) && "sam".equals(saved.getAuthorId())));
    }

    @Test
    void publishInGroup_NonMemberIsForbidden() {
        assertThrows(ForbiddenOperationException.class, () -> service.publishInGroup("mallory", groupPost()));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void commentInGroup_NonMemberIsForbidden() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.commentInGroup("mallory", groupComment(groupPostId)));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void commentInGroup_PostOfAnotherGroupIsForbidden() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.commentInGroup("sam", groupComment(otherGroupPostId)));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void commentOnUserPage_OnAGroupPostIsForbidden() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.commentOnUserPage("mallory", comment(groupPostId)));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void commentOnUserPage_ToAPublicationThatIsGone_IsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.commentOnUserPage("sam", comment(404L)));
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void commentOnUserPage_UnderAComment_Succeeds() throws Exception {
        service.commentOnUserPage("mallory", comment(commentId));

        verify(commentsRepository).save(argThat(saved ->
                "mallory".equals(saved.getAuthorId()) && commentId.equals(saved.getParentId())
                        && postId.equals(saved.getRootId())));
    }

    // ---------- where a thread lives ----------

    @Test
    void placeOf_AComment_IsThePageOfItsPost() {
        assertEquals(new ThreadPlace(null, "alex"), service.placeOf(stored.get(commentId)));
        assertEquals(new ThreadPlace(groupId, "sam"), service.placeOf(stored.get(groupPostId)));
    }
}

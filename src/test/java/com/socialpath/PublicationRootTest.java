package com.socialpath;

import com.socialpath.config.FeedProperties;
import com.socialpath.dto.request.NewComment;
import com.socialpath.entity.Publication;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ThreadPlace;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Where a thread lives is read from its post, found through the comment's
 * root_id in one lookup at any depth; a new comment takes its root from its
 * parent.
 */
class PublicationRootTest {

    private CommentsRepository commentsRepository;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        service = new CommentsServiceImpl(
                mock(UserRepository.class),
                commentsRepository,
                mock(FileStorageService.class),
                mock(VideoService.class), new FeedProperties(20, 10, 1),
                mock(GroupService.class));
    }

    private Publication publication(Long id, String author, Long parentId, Long rootId) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setAuthorId(author);
        publication.setParentId(parentId);
        publication.setRootId(rootId);
        when(commentsRepository.findById(id)).thenReturn(Optional.of(publication));
        return publication;
    }

    @Test
    void placeOf_APost_NeedsNoLookup() {
        Publication post = publication(1L, "alex", null, null);

        assertEquals(new ThreadPlace(null, "alex"), service.placeOf(post));
        verify(commentsRepository, times(0)).findById(any());
    }

    @Test
    void placeOf_ADeepReply_IsOneLookupOfItsPost() {
        publication(1L, "alex", null, null);
        Publication reply = publication(500L, "sam", 499L, 1L);

        assertEquals(new ThreadPlace(null, "alex"), service.placeOf(reply));
        verify(commentsRepository, times(1)).findById(any());
    }

    @Test
    void aNewComment_TakesTheRootFromItsParent() throws Exception {
        publication(1L, "alex", null, null);
        publication(10L, "sam", 1L, 1L);
        when(commentsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.commentOnUserPage("sam", comment(1L));
        service.commentOnUserPage("alex", comment(10L));

        ArgumentCaptor<Publication> saved = ArgumentCaptor.forClass(Publication.class);
        verify(commentsRepository, times(2)).save(saved.capture());
        // A reply to the post has the post as its root; a reply to a reply, the same root.
        assertEquals(1L, saved.getAllValues().get(0).getRootId());
        assertEquals(1L, saved.getAllValues().get(1).getRootId());
        assertEquals(10L, saved.getAllValues().get(1).getParentId());
    }

    private static NewComment comment(Long parentId) {
        NewComment comment = new NewComment();
        comment.setIdPublication(parentId);
        comment.setText("text");
        return comment;
    }
}

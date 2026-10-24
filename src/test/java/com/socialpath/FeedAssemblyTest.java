package com.socialpath;

import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.entity.Publication;
import com.socialpath.repository.AuthorView;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The feed is loaded with a fixed number of queries and assembled in memory. */
class FeedAssemblyTest {

    private record Author(String getLogin, String getFirstName, String getLastName, String getImageId)
            implements AuthorView {
    }

    private CommentsRepository commentsRepository;
    private UserRepository userRepository;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        userRepository = mock(UserRepository.class);
        service = new CommentsServiceImpl(userRepository, commentsRepository, mock(FileStorageService.class));
    }

    private Publication publication(Long id, Long parentId, String author, String text) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setParentId(parentId);
        publication.setAuthorId(author);
        publication.setText(text);
        return publication;
    }

    @Test
    void loadUserFeed_BuildsTheCommentTreeFromOneRepliesQuery() {
        when(commentsRepository.findUserFeed("anna")).thenReturn(List.of(
                publication(1L, null, "anna", "first post"),
                publication(2L, null, "anna", "second post")));
        when(commentsRepository.findReplies(List.of(1L, 2L))).thenReturn(List.of(
                publication(10L, 1L, "boris", "comment"),
                publication(11L, 10L, "anna", "reply to the comment"),
                publication(12L, 1L, "ghost", "second comment")));
        when(userRepository.findAuthors(any())).thenReturn(List.of(
                new Author("anna", "Anna", "Example", "anna.jpg"),
                new Author("boris", "Boris", "Example", "")));

        List<PublicationPresentable> feed = service.loadUserFeed("anna");

        assertEquals(List.of("first post", "second post"), feed.stream().map(PublicationPresentable::getText).toList());
        PublicationPresentable first = feed.get(0);
        assertEquals("Anna Example", first.getAuthorName());
        assertEquals("anna.jpg", first.getAuthorImageId());
        assertEquals(List.of("comment", "second comment"),
                first.getPublications().stream().map(PublicationPresentable::getText).toList());

        PublicationPresentable comment = first.getPublications().get(0);
        assertEquals("Boris Example", comment.getAuthorName());
        assertNull(comment.getAuthorImageId());
        assertEquals("reply to the comment", comment.getPublications().get(0).getText());
        // An author that no longer exists is shown by login.
        assertEquals("ghost", first.getPublications().get(1).getAuthorName());
        assertTrue(feed.get(1).getPublications().isEmpty());

        verify(commentsRepository, times(1)).findReplies(any());
        verify(userRepository, times(1)).findAuthors(any());
        verify(commentsRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void loadGroupFeed_WithoutPosts_RunsNoFurtherQueries() {
        when(commentsRepository.findGroupFeed(7L)).thenReturn(List.of());

        assertTrue(service.loadGroupFeed(7L).isEmpty());

        verify(commentsRepository, never()).findReplies(any());
        verify(userRepository, never()).findAuthors(any());
    }
}

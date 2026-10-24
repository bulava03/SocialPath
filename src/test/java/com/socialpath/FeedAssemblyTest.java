package com.socialpath;

import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.entity.Publication;
import com.socialpath.repository.AuthorView;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CommentsService;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        when(commentsRepository.findUserFeed(eq("anna"), isNull(), any())).thenReturn(List.of(
                publication(1L, null, "anna", "first post"),
                publication(2L, null, "anna", "second post")));
        when(commentsRepository.findReplies(List.of(1L, 2L))).thenReturn(List.of(
                publication(10L, 1L, "boris", "comment"),
                publication(11L, 10L, "anna", "reply to the comment"),
                publication(12L, 1L, "ghost", "second comment")));
        when(userRepository.findAuthors(any())).thenReturn(List.of(
                new Author("anna", "Anna", "Example", "anna.jpg"),
                new Author("boris", "Boris", "Example", "")));

        List<PublicationPresentable> feed = service.loadUserFeed("anna", null).posts();

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
        when(commentsRepository.findGroupFeed(eq(7L), isNull(), any())).thenReturn(List.of());

        FeedPage page = service.loadGroupFeed(7L, null);

        assertTrue(page.posts().isEmpty());
        assertNull(page.nextBefore());

        verify(commentsRepository, never()).findReplies(any());
        verify(userRepository, never()).findAuthors(any());
    }

    @Test
    void loadUserFeed_AsksForOneExtraPostToKnowWhetherMoreExist() {
        List<Publication> rows = new ArrayList<>();
        for (long id = 60; id > 60 - CommentsService.FEED_PAGE_SIZE - 1; id--) {
            rows.add(publication(id, null, "anna", "post " + id));
        }
        when(commentsRepository.findUserFeed(eq("anna"), eq(61L), any())).thenReturn(rows);

        FeedPage page = service.loadUserFeed("anna", 61L);

        assertEquals(CommentsService.FEED_PAGE_SIZE, page.posts().size());
        assertEquals(60L, page.posts().get(0).getId());
        // The 21st row is not shown; the next portion continues below the 20th.
        assertEquals(41L, page.nextBefore());
        verify(commentsRepository).findUserFeed("anna", 61L, Limit.of(CommentsService.FEED_PAGE_SIZE + 1));
    }

    @Test
    void loadGroupFeed_LastPortion_HasNothingToContinueFrom() {
        when(commentsRepository.findGroupFeed(eq(7L), eq(3L), any())).thenReturn(List.of(
                publication(2L, null, "anna", "older"),
                publication(1L, null, "anna", "oldest")));

        FeedPage page = service.loadGroupFeed(7L, 3L);

        assertEquals(2, page.posts().size());
        assertNull(page.nextBefore());
    }

    @Test
    void latestActivity_WithoutActivityByOthers_IsZero() {
        when(commentsRepository.findLatestGroupFeedActivity(7L, "anna")).thenReturn(null);
        when(commentsRepository.findLatestUserFeedActivity("anna", "boris")).thenReturn(42L);

        assertEquals(0, service.latestGroupFeedActivity(7L, "anna"));
        assertEquals(42, service.latestUserFeedActivity("anna", "boris"));
    }

    @Test
    void loadThread_OfAComment_RendersTheWholePost() {
        Publication post = publication(1L, null, "anna", "post");
        Publication comment = publication(10L, 1L, "boris", "comment");
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentsRepository.findReplies(List.of(1L))).thenReturn(List.of(comment));

        PublicationPresentable thread = service.loadThread(comment);

        assertEquals("post", thread.getText());
        assertEquals("comment", thread.getPublications().get(0).getText());
    }
}

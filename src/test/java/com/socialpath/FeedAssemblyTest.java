package com.socialpath;

import com.socialpath.config.FeedProperties;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.dto.response.RepliesPage;
import com.socialpath.entity.Publication;
import com.socialpath.repository.AuthorView;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.ReplyCount;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The feed is loaded in bounded portions, with a number of queries that
 * depends on how many reply levels are loaded, not on how many posts or
 * replies there are; the tree is assembled in memory.
 */
class FeedAssemblyTest {

    private static final int PAGE_SIZE = 20;
    private static final int REPLIES_PAGE_SIZE = 2;

    private record Author(String getLogin, String getFirstName, String getLastName, String getImageId)
            implements AuthorView {
    }

    private record Count(Long getParentId, Long getTotal) implements ReplyCount {
    }

    private CommentsRepository commentsRepository;
    private UserRepository userRepository;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        userRepository = mock(UserRepository.class);
        service = service(1);
    }

    private CommentsServiceImpl service(int preloadDepth) {
        return new CommentsServiceImpl(userRepository, commentsRepository, mock(FileStorageService.class),
                mock(VideoService.class), new FeedProperties(PAGE_SIZE, REPLIES_PAGE_SIZE, preloadDepth));
    }

    private Publication publication(Long id, Long parentId, String author, String text) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setParentId(parentId);
        publication.setAuthorId(author);
        publication.setText(text);
        return publication;
    }

    private static List<String> texts(List<PublicationPresentable> publications) {
        return publications.stream().map(PublicationPresentable::getText).toList();
    }

    @Test
    void loadUserFeed_LoadsOnePortionOfDirectReplies_AndOnlyCountsTheRest() {
        when(commentsRepository.findUserFeed(eq("alex"), isNull(), any())).thenReturn(List.of(
                publication(2L, null, "alex", "newer post"),
                publication(1L, null, "alex", "older post")));
        // "newer post" has three replies; the newest two come, newest first.
        when(commentsRepository.findNewestReplies(List.of(2L, 1L), REPLIES_PAGE_SIZE)).thenReturn(List.of(
                publication(12L, 2L, "ghost", "third reply"),
                publication(11L, 2L, "sam", "second reply")));
        when(commentsRepository.countReplies(List.of(2L, 1L, 12L, 11L))).thenReturn(List.of(
                new Count(2L, 3L),
                new Count(11L, 4L)));
        when(userRepository.findAuthors(any())).thenReturn(List.of(
                new Author("alex", "Alex", "Example", "alex.jpg"),
                new Author("sam", "Sam", "Example", "")));

        List<PublicationPresentable> feed = service.loadUserFeed("alex", null).posts();

        assertEquals(List.of("newer post", "older post"), texts(feed));
        PublicationPresentable newer = feed.get(0);
        assertEquals("Alex Example", newer.getAuthorName());
        assertEquals("alex.jpg", newer.getAuthorImageId());
        assertEquals(List.of("third reply", "second reply"), texts(newer.getPublications()));
        // The oldest reply stays behind a button that continues below the loaded ones.
        assertEquals(1, newer.getHiddenReplies());
        assertEquals(1, newer.getNextReplies());
        assertEquals(11L, newer.getRepliesBefore());

        // A reply's own replies are not loaded, only counted.
        PublicationPresentable reply = newer.getPublications().get(1);
        assertEquals("Sam Example", reply.getAuthorName());
        assertNull(reply.getAuthorImageId());
        assertTrue(reply.getPublications().isEmpty());
        assertEquals(4, reply.getHiddenReplies());
        // More than a portion: the next click brings only REPLIES_PAGE_SIZE of them.
        assertEquals(REPLIES_PAGE_SIZE, reply.getNextReplies());
        assertNull(reply.getRepliesBefore());
        // An author that no longer exists is shown by login.
        assertEquals("ghost", newer.getPublications().get(0).getAuthorName());

        PublicationPresentable older = feed.get(1);
        assertTrue(older.getPublications().isEmpty());
        assertEquals(0, older.getHiddenReplies());

        verify(commentsRepository, times(1)).findNewestReplies(any(), anyInt());
        verify(commentsRepository, times(1)).countReplies(any());
        verify(userRepository, times(1)).findAuthors(any());
        verify(commentsRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void preloadDepth_LoadsOneQueryPerLevel() {
        service = service(2);
        when(commentsRepository.findUserFeed(eq("alex"), isNull(), any())).thenReturn(List.of(
                publication(1L, null, "alex", "post")));
        when(commentsRepository.findNewestReplies(List.of(1L), REPLIES_PAGE_SIZE)).thenReturn(List.of(
                publication(10L, 1L, "sam", "comment")));
        when(commentsRepository.findNewestReplies(List.of(10L), REPLIES_PAGE_SIZE)).thenReturn(List.of(
                publication(20L, 10L, "alex", "reply to the comment")));
        when(commentsRepository.countReplies(List.of(1L, 10L, 20L))).thenReturn(List.of(
                new Count(1L, 1L), new Count(10L, 1L), new Count(20L, 5L)));

        PublicationPresentable post = service.loadUserFeed("alex", null).posts().get(0);

        PublicationPresentable comment = post.getPublications().get(0);
        assertEquals("reply to the comment", comment.getPublications().get(0).getText());
        assertEquals(0, comment.getHiddenReplies());
        // The third level is only counted.
        assertEquals(5, comment.getPublications().get(0).getHiddenReplies());
        verify(commentsRepository, times(2)).findNewestReplies(any(), anyInt());
    }

    @Test
    void preloadDepthZero_ShowsPostsWithTheirRepliesCounted() {
        service = service(0);
        when(commentsRepository.findGroupFeed(eq(7L), isNull(), any())).thenReturn(List.of(
                publication(1L, null, "alex", "post")));
        when(commentsRepository.countReplies(List.of(1L))).thenReturn(List.of(new Count(1L, 30L)));

        PublicationPresentable post = service.loadGroupFeed(7L, null).posts().get(0);

        assertTrue(post.getPublications().isEmpty());
        assertEquals(30, post.getHiddenReplies());
        verify(commentsRepository, never()).findNewestReplies(any(), anyInt());
    }

    @Test
    void loadGroupFeed_WithoutPosts_RunsNoFurtherQueries() {
        when(commentsRepository.findGroupFeed(eq(7L), isNull(), any())).thenReturn(List.of());

        FeedPage page = service.loadGroupFeed(7L, null);

        assertTrue(page.posts().isEmpty());
        assertNull(page.nextBefore());

        verify(commentsRepository, never()).findNewestReplies(any(), anyInt());
        verify(commentsRepository, never()).countReplies(any());
        verify(userRepository, never()).findAuthors(any());
    }

    @Test
    void loadUserFeed_AsksForOneExtraPostToKnowWhetherMoreExist() {
        List<Publication> rows = new ArrayList<>();
        for (long id = 60; id > 60 - PAGE_SIZE - 1; id--) {
            rows.add(publication(id, null, "alex", "post " + id));
        }
        when(commentsRepository.findUserFeed(eq("alex"), eq(61L), any())).thenReturn(rows);

        FeedPage page = service.loadUserFeed("alex", 61L);

        assertEquals(PAGE_SIZE, page.posts().size());
        assertEquals(60L, page.posts().get(0).getId());
        // The 21st row is not shown; the next portion continues below the 20th.
        assertEquals(41L, page.nextBefore());
        verify(commentsRepository).findUserFeed("alex", 61L, Limit.of(PAGE_SIZE + 1));
    }

    @Test
    void loadGroupFeed_LastPortion_HasNothingToContinueFrom() {
        when(commentsRepository.findGroupFeed(eq(7L), eq(3L), any())).thenReturn(List.of(
                publication(2L, null, "alex", "older"),
                publication(1L, null, "alex", "oldest")));

        FeedPage page = service.loadGroupFeed(7L, 3L);

        assertEquals(2, page.posts().size());
        assertNull(page.nextBefore());
    }

    @Test
    void loadReplies_ReturnsOnePortion_AndHowManyAreLeftBelowIt() {
        when(commentsRepository.findReplies(5L, 30L, Limit.of(REPLIES_PAGE_SIZE))).thenReturn(List.of(
                publication(29L, 5L, "sam", "newer"),
                publication(27L, 5L, "alex", "older")));
        when(commentsRepository.countByParentIdAndIdLessThan(5L, 27L)).thenReturn(4L);
        when(commentsRepository.countReplies(List.of(29L, 27L))).thenReturn(List.of(new Count(27L, 2L)));

        RepliesPage page = service.loadReplies(5L, 30L);

        assertEquals(List.of("newer", "older"), texts(page.replies()));
        assertEquals(27L, page.nextBefore());
        assertEquals(4, page.hidden());
        assertEquals(REPLIES_PAGE_SIZE, page.next());
        // One level at a time: the replies of these replies are only counted.
        assertTrue(page.replies().get(1).getPublications().isEmpty());
        assertEquals(2, page.replies().get(1).getHiddenReplies());
        verify(commentsRepository, never()).findNewestReplies(any(), anyInt());
    }

    @Test
    void loadReplies_LastPortion_HasNothingToContinueFrom() {
        when(commentsRepository.findReplies(eq(5L), isNull(), any())).thenReturn(List.of(
                publication(29L, 5L, "sam", "only")));
        when(commentsRepository.countByParentIdAndIdLessThan(5L, 29L)).thenReturn(0L);

        RepliesPage page = service.loadReplies(5L, null);

        assertEquals(1, page.replies().size());
        assertNull(page.nextBefore());
        assertEquals(0, page.hidden());
        assertEquals(0, page.next());
    }

    @Test
    void loadReplies_WithoutReplies_RunsNoFurtherQueries() {
        when(commentsRepository.findReplies(eq(5L), isNull(), any())).thenReturn(List.of());

        RepliesPage page = service.loadReplies(5L, null);

        assertTrue(page.replies().isEmpty());
        assertNull(page.nextBefore());
        verify(commentsRepository, never()).countByParentIdAndIdLessThan(anyLong(), anyLong());
        verify(userRepository, never()).findAuthors(any());
    }

    @Test
    void loadPublication_CountsItsRepliesWithoutLoadingThem() {
        Publication comment = publication(10L, 1L, "sam", "comment");
        when(commentsRepository.countReplies(List.of(10L))).thenReturn(List.of(new Count(10L, 3L)));

        PublicationPresentable presented = service.loadPublication(comment);

        assertEquals("comment", presented.getText());
        assertTrue(presented.getPublications().isEmpty());
        assertEquals(3, presented.getHiddenReplies());
        assertNull(presented.getRepliesBefore());
        verify(commentsRepository, never()).findNewestReplies(any(), anyInt());
        verify(commentsRepository, never()).findById(any());
    }

    @Test
    void latestActivity_WithoutActivityByOthers_IsZero() {
        when(commentsRepository.findLatestGroupPost(7L, "alex")).thenReturn(null);
        when(commentsRepository.findLatestUserPost("alex", "sam")).thenReturn(42L);

        assertEquals(0, service.latestGroupFeedActivity(7L, "alex"));
        assertEquals(42, service.latestUserFeedActivity("alex", "sam"));
    }
}

package com.socialpath;

import com.socialpath.entity.Publication;
import com.socialpath.repository.CommentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The feed and deletion queries on the self-referencing publications table:
 * recursive CTEs and the ON DELETE CASCADE the deletion relies on.
 */
class CommentsRepositoryTest extends MySqlRepositoryTest {

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final AtomicInteger clock = new AtomicInteger();

    @BeforeEach
    void users() {
        user("anna");
        user("ben");
    }

    /** Saves a publication; each one is a second newer than the previous. */
    private Long publish(String author, Long parentId, Long groupId, String... media) {
        Publication publication = new Publication();
        publication.setAuthorId(author);
        publication.setParentId(parentId);
        publication.setGroupId(groupId);
        publication.setText("text");
        publication.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(clock.incrementAndGet()));
        publication.setMedia(List.of(media));
        return commentsRepository.saveAndFlush(publication).getId();
    }

    private Long group(String owner) {
        jdbc.update("INSERT INTO social_groups (name, owner_login) VALUES ('Group', ?)", owner);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    void deletingAPost_RemovesTheWholeThreadWithItsMedia_AndNothingElse() {
        Long post = publish("anna", null, null, "post.jpg");
        Long comment = publish("ben", post, null, "comment.jpg");
        Long reply = publish("anna", comment, null, "reply-1.jpg", "reply-2.jpg");
        Long otherPost = publish("anna", null, null, "other.jpg");

        // The files to clean up are collected from the whole subtree first.
        assertThat(commentsRepository.findSubtreeMediaFileIds(post))
                .containsExactlyInAnyOrder("post.jpg", "comment.jpg", "reply-1.jpg", "reply-2.jpg");

        commentsRepository.deleteById(post);
        entityManager.flush();
        entityManager.clear();

        // One delete of the root; the database cascade took the replies and media rows.
        assertThat(count("SELECT COUNT(*) FROM publications WHERE id IN (?, ?, ?)", post, comment, reply)).isZero();
        assertThat(count("SELECT COUNT(*) FROM publication_media WHERE publication_id IN (?, ?, ?)",
                post, comment, reply)).isZero();
        assertThat(commentsRepository.findById(otherPost)).isPresent();
        assertThat(commentsRepository.findSubtreeMediaFileIds(otherPost)).containsExactly("other.jpg");
    }

    @Test
    void deletingAComment_KeepsThePostAndTheOtherComments() {
        Long post = publish("anna", null, null);
        Long comment = publish("ben", post, null, "comment.jpg");
        Long reply = publish("anna", comment, null, "reply.jpg");
        Long sibling = publish("ben", post, null);

        assertThat(commentsRepository.findSubtreeMediaFileIds(comment))
                .containsExactlyInAnyOrder("comment.jpg", "reply.jpg");

        commentsRepository.deleteById(comment);
        entityManager.flush();
        entityManager.clear();

        assertThat(commentsRepository.findById(post)).isPresent();
        assertThat(commentsRepository.findById(sibling)).isPresent();
        assertThat(commentsRepository.findById(reply)).isEmpty();
    }

    @Test
    void findReplies_ReturnsEveryDepthOfSeveralPosts_OldestFirst() {
        Long first = publish("anna", null, null);
        Long second = publish("anna", null, null);
        Long a = publish("ben", first, null);
        Long b = publish("anna", second, null);
        Long c = publish("ben", a, null);
        Long d = publish("anna", c, null);
        Long unrelated = publish("ben", publish("ben", null, null), null);

        List<Long> replies = commentsRepository.findReplies(List.of(first, second)).stream()
                .map(Publication::getId).toList();

        assertThat(replies).containsExactly(a, b, c, d).doesNotContain(unrelated);
    }

    @Test
    void userFeed_HasOnlyTheOwnersTopLevelPagePosts_InNewestFirstPortions() {
        Long groupId = group("anna");
        Long p1 = publish("anna", null, null);
        Long p2 = publish("anna", null, null);
        Long p3 = publish("anna", null, null);
        publish("anna", p1, null);          // a comment is not a post
        publish("anna", null, groupId);     // a group post is not on the user page
        publish("ben", null, null);         // another user's page

        List<Publication> firstPortion = commentsRepository.findUserFeed("anna", null, Limit.of(2));
        assertThat(firstPortion).extracting(Publication::getId).containsExactly(p3, p2);

        Long before = firstPortion.get(firstPortion.size() - 1).getId();
        assertThat(commentsRepository.findUserFeed("anna", before, Limit.of(2)))
                .extracting(Publication::getId).containsExactly(p1);
    }

    @Test
    void groupFeed_HasOnlyThatGroupsTopLevelPosts() {
        Long groupId = group("anna");
        Long otherGroup = group("ben");
        Long post = publish("ben", null, groupId);
        publish("anna", post, null);
        publish("ben", null, otherGroup);
        publish("anna", null, null);

        assertThat(commentsRepository.findGroupFeed(groupId, null, Limit.of(10)))
                .extracting(Publication::getId).containsExactly(post);
    }

    @Test
    void latestActivity_CountsOthersAtAnyDepth_ButNotTheViewer() {
        Long post = publish("anna", null, null);
        Long comment = publish("ben", post, null);
        publish("anna", comment, null);     // the viewer's own reply is newer, but not news

        assertThat(commentsRepository.findLatestUserFeedActivity("anna", "anna")).isEqualTo(comment);
        assertThat(commentsRepository.findLatestUserFeedActivity("ben", "anna")).isNull();
    }
}

package com.socialpath;

import com.socialpath.entity.Publication;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.ReplyCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Limit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The feed and deletion queries on the self-referencing publications table:
 * window functions, paging by id, and deleting threads of any depth.
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
        if (parentId != null) {
            // As the service does: the parent's root, or the parent when it is the post.
            publication.setRootId(jdbc.queryForObject(
                    "SELECT COALESCE(root_id, id) FROM publications WHERE id = ?", Long.class, parentId));
        }
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

    /** Runs the stored procedure delete_thread and forgets what Hibernate had loaded. */
    private List<String> deleteThread(Long id) {
        List<String> mediaIds = commentsRepository.deleteThread(id);
        entityManager.clear();
        return mediaIds;
    }

    @Test
    void deleteThread_RemovesThePostWithItsWholeTree_AndReturnsItsMedia() {
        Long post = publish("anna", null, null, "post.jpg");
        Long comment = publish("ben", post, null, "comment.jpg");
        Long reply = publish("anna", comment, null, "reply-1.jpg", "reply-2.jpg");
        Long otherPost = publish("anna", null, null, "other.jpg");

        assertThat(deleteThread(post))
                .containsExactlyInAnyOrder("post.jpg", "comment.jpg", "reply-1.jpg", "reply-2.jpg");

        assertThat(count("SELECT COUNT(*) FROM publications WHERE id IN (?, ?, ?)", post, comment, reply)).isZero();
        assertThat(count("SELECT COUNT(*) FROM publication_media WHERE publication_id IN (?, ?, ?)",
                post, comment, reply)).isZero();
        assertThat(commentsRepository.findById(otherPost)).isPresent();
        assertThat(count("SELECT COUNT(*) FROM publication_media WHERE publication_id = ?", otherPost)).isEqualTo(1);
    }

    @Test
    void deleteThread_OfAComment_KeepsThePostAndTheOtherComments() {
        Long post = publish("anna", null, null);
        Long comment = publish("ben", post, null, "comment.jpg");
        Long reply = publish("anna", comment, null, "reply.jpg");
        Long sibling = publish("ben", post, null);

        assertThat(deleteThread(comment)).containsExactlyInAnyOrder("comment.jpg", "reply.jpg");

        assertThat(commentsRepository.findById(post)).isPresent();
        assertThat(commentsRepository.findById(sibling)).isPresent();
        assertThat(commentsRepository.findById(comment)).isEmpty();
        assertThat(commentsRepository.findById(reply)).isEmpty();
    }

    @Test
    void deleteThread_DeeperThanAnyDatabaseLimit_RemovesEverything() {
        // Deeper than InnoDB's 15 nested cascades, and three steps of the
        // procedure's walk, each of which stays under MySQL's 1000 recursion steps.
        Long post = publish("anna", null, null, "post.jpg");
        Long parent = post;
        for (int depth = 1; depth <= 2500; depth++) {
            String author = depth % 2 == 0 ? "anna" : "ben";
            parent = depth % 500 == 0
                    ? publish(author, parent, null, "deep-" + depth + ".jpg")
                    : publish(author, parent, null);
        }
        Long otherPost = publish("ben", null, null);

        assertThat(deleteThread(post)).containsExactlyInAnyOrder("post.jpg",
                "deep-500.jpg", "deep-1000.jpg", "deep-1500.jpg", "deep-2000.jpg", "deep-2500.jpg");

        assertThat(count("SELECT COUNT(*) FROM publications")).isEqualTo(1);
        assertThat(commentsRepository.findById(otherPost)).isPresent();
        assertThat(count("SELECT COUNT(*) FROM publication_media")).isZero();
    }

    @Test
    void aComment_MustHaveARoot_AndAPostMustNot() {
        Long post = publish("anna", null, null);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO publications (author_login, parent_id, text, created_at)
                VALUES ('ben', ?, 'no root', NOW())
                """, post)).hasMessageContaining("chk_publications_root");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO publications (author_login, root_id, text, created_at)
                VALUES ('ben', ?, 'a post with a root', NOW())
                """, post)).hasMessageContaining("chk_publications_root");
    }

    @Test
    void deleteThread_OfAPublicationThatIsGone_DeletesNothing() {
        Long post = publish("anna", null, null);

        assertThat(deleteThread(post + 1000)).isEmpty();
        assertThat(commentsRepository.findById(post)).isPresent();
    }

    @Test
    void findNewestReplies_TakesAtMostThatManyDirectRepliesOfEachParent_NewestFirst() {
        Long first = publish("anna", null, null);
        Long second = publish("anna", null, null);
        Long a1 = publish("ben", first, null);
        Long a2 = publish("ben", first, null);
        Long a3 = publish("ben", first, null);
        Long b1 = publish("anna", second, null);
        publish("anna", a3, null);                              // a reply to a reply is a level deeper
        publish("ben", publish("ben", null, null), null);       // another post's reply

        List<Long> replies = commentsRepository.findNewestReplies(List.of(first, second), 2).stream()
                .map(Publication::getId).toList();

        assertThat(replies).containsExactly(b1, a3, a2).doesNotContain(a1);
    }

    @Test
    void findReplies_PagesThroughTheDirectRepliesOfOneParent_NewestFirst() {
        Long post = publish("anna", null, null);
        Long r1 = publish("ben", post, null);
        Long r2 = publish("anna", post, null);
        Long r3 = publish("ben", post, null);
        publish("anna", r3, null);                              // not a direct reply

        List<Publication> firstPortion = commentsRepository.findReplies(post, null, Limit.of(2));
        assertThat(firstPortion).extracting(Publication::getId).containsExactly(r3, r2);
        assertThat(commentsRepository.countByParentIdAndIdLessThan(post, r2)).isEqualTo(1);

        assertThat(commentsRepository.findReplies(post, r2, Limit.of(2)))
                .extracting(Publication::getId).containsExactly(r1);
        assertThat(commentsRepository.countByParentIdAndIdLessThan(post, r1)).isZero();
    }

    @Test
    void countReplies_CountsOnlyDirectReplies_AndLeavesOutParentsWithout() {
        Long post = publish("anna", null, null);
        Long comment = publish("ben", post, null);
        publish("anna", post, null);
        publish("anna", comment, null);
        Long lonely = publish("ben", null, null);

        assertThat(commentsRepository.countReplies(List.of(post, comment, lonely)))
                .extracting(ReplyCount::getParentId, ReplyCount::getTotal)
                .containsExactlyInAnyOrder(tuple(post, 2L), tuple(comment, 1L));
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
    void latestPost_IsTheNewestPostByOthers_CommentsAreNotCounted() {
        Long groupId = group("anna");
        Long userPost = publish("anna", null, null);
        publish("ben", userPost, null);                 // a comment is not news
        Long groupPost = publish("ben", null, groupId);
        publish("anna", null, groupId);                 // the viewer's own post is not news
        publish("ben", groupPost, null);

        assertThat(commentsRepository.findLatestUserPost("anna", "ben")).isEqualTo(userPost);
        assertThat(commentsRepository.findLatestUserPost("anna", "anna")).isNull();
        assertThat(commentsRepository.findLatestGroupPost(groupId, "anna")).isEqualTo(groupPost);
    }
}

package com.socialpath.service;

import com.socialpath.entity.Publication;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;

import java.io.IOException;
import java.util.List;

/**
 * The publication/comment feed. Publications and comments share one
 * self-referencing table (a {@link Publication} with an optional parent), so
 * the same operations serve both a top-level post and a nested comment.
 */
public interface CommentsService {

    /** How many posts a feed portion holds. */
    int FEED_PAGE_SIZE = 20;

    /**
     * Creates a publication on a user's page.
     * @param newPublication the publication content and target
     * @return the id of the new publication
     */
    Long addNewUserPublication(NewPublication newPublication) throws IOException;

    /**
     * Creates a publication on a group's page.
     * @param newPublication the publication content and target
     * @return the id of the new publication
     */
    Long addNewGroupPublication(NewPublication newPublication) throws IOException;

    /**
     * Loads a portion of a user page's feed with nested comments.
     * @param login the page owner
     * @param before continue below this post id; null for the newest posts
     * @return up to {@link #FEED_PAGE_SIZE} posts, newest first
     */
    FeedPage loadUserFeed(String login, Long before);

    /**
     * Loads a portion of a group page's feed with nested comments.
     * @param groupId the group id
     * @param before continue below this post id; null for the newest posts
     * @return up to {@link #FEED_PAGE_SIZE} posts, newest first
     */
    FeedPage loadGroupFeed(Long groupId, Long before);

    /**
     * Loads one post with its whole comment tree, to refresh it in place.
     * @param post the post itself or any comment under it
     * @return the top-level post as a view model
     */
    PublicationPresentable loadThread(Publication post);

    /**
     * The newest activity by others in a user page's feed, posts and
     * comments alike.
     * @param owner the page owner
     * @param viewer the signed-in user, whose own activity is not news
     * @return the largest such publication id, 0 when there is none
     */
    long latestUserFeedActivity(String owner, String viewer);

    /**
     * The newest activity by others in a group's feed.
     * @param groupId the group id
     * @param viewer the signed-in user, whose own activity is not news
     * @return the largest such publication id, 0 when there is none
     */
    long latestGroupFeedActivity(Long groupId, String viewer);

    /**
     * Looks up a single publication or comment by id.
     * @param id the publication id
     * @return the publication, or null if none exists
     */
    Publication findById(Long id);

    /**
     * Walks up from a comment to the top-level publication it belongs to.
     * The root tells where the thread lives: a group (groupId set) or a user
     * page (owned by the root's author).
     * @param publication a publication or a comment at any depth
     * @return the top-level publication; the argument itself when it has no parent
     */
    Publication findRoot(Publication publication);


    /**
     * Adds a comment under an existing publication.
     * @param newComment the comment content and parent
     */
    void addNewComment(NewComment newComment) throws IOException;

    /**
     * Deletes a publication or a comment, together with its whole reply
     * subtree and all associated media files. Permission checks are the
     * caller's job.
     * @param id the publication or comment id
     */
    void deletePublication(Long id);
}

package com.socialpath.service;

import com.socialpath.entity.Publication;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.dto.response.RepliesPage;

import java.io.IOException;

/**
 * The publication/comment feed. Publications and comments share one
 * self-referencing table (a {@link Publication} with an optional parent), so
 * the same operations serve both a top-level post and a nested comment.
 * <p>
 * Writing and deleting check permissions here, on stored data: where a
 * comment lives is read from the post at the top of its thread, never from
 * ids the client sends along. A refused operation throws
 * {@link com.socialpath.exception.ForbiddenOperationException}; a request
 * with nothing to publish or a missing target, IllegalArgumentException.
 */
public interface CommentsService {

    /**
     * Publishes a post on the author's own page.
     * @param author the signed-in user
     * @param request the text and media
     * @return the id of the new post
     */
    Long publishOnUserPage(String author, NewPublication request) throws IOException;

    /**
     * Publishes a post in a group; only members may.
     * @param author the signed-in user
     * @param request the group, text and media
     * @return the id of the new post
     */
    Long publishInGroup(String author, NewPublication request) throws IOException;

    /**
     * Replies to a post or comment on a user page; anyone signed in may.
     * Threads in groups are refused here: they go through
     * {@link #commentInGroup}, which checks membership.
     * @param author the signed-in user
     * @param request the post or comment replied to, text and media
     * @return the id of the new comment
     */
    Long commentOnUserPage(String author, NewComment request) throws IOException;

    /**
     * Replies to a post or comment in a group; only members may, and only
     * to what is in that group.
     * @param author the signed-in user
     * @param request the group, the post or comment replied to, text and media
     * @return the id of the new comment
     */
    Long commentInGroup(String author, NewComment request) throws IOException;

    /**
     * Deletes a post or comment on a user page with all of its replies. The
     * page owner may delete anything in their threads, everyone else only
     * their own posts and comments.
     * @param login the signed-in user
     * @param id the post or comment
     */
    void deleteFromUserPage(String login, Long id);

    /**
     * Deletes a post or comment in a group with all of its replies. Allowed
     * for its author and for the group's owner and admins.
     * @param login the signed-in user
     * @param groupId the group named in the request
     * @param id the post or comment
     */
    void deleteFromGroup(String login, String groupId, Long id);

    /**
     * Where a post or comment lives, read from the post at the top of its
     * thread: to render it as on its page.
     * @param publication a post or a comment at any depth
     * @return its group, or the user whose page it is
     */
    ThreadPlace placeOf(Publication publication);

    /**
     * Loads a portion of a user page's feed with the first portions of its
     * replies, as many levels deep as app.feed.replies-preload-depth says.
     * @param login the page owner
     * @param before continue below this post id; null for the newest posts
     * @return up to app.feed.page-size posts, newest first
     */
    FeedPage loadUserFeed(String login, Long before);

    /**
     * Loads a portion of a group page's feed, like {@link #loadUserFeed}.
     * @param groupId the group id
     * @param before continue below this post id; null for the newest posts
     * @return up to app.feed.page-size posts, newest first
     */
    FeedPage loadGroupFeed(Long groupId, Long before);

    /**
     * Loads a portion of the direct replies of a post or comment. Their own
     * replies are only counted: each further level opens on request.
     * @param parentId the post or comment
     * @param before continue below this reply id; null for the newest replies
     * @return up to app.feed.replies-page-size replies, newest first
     */
    RepliesPage loadReplies(Long parentId, Long before);

    /**
     * Loads a single post or comment, to show a new one or refresh one in
     * place. Its replies are only counted.
     * @param publication the post or comment
     * @return its view model
     */
    PublicationPresentable loadPublication(Publication publication);

    /**
     * The newest activity by others in a user page's feed: new posts only.
     * Comments are not watched; a reload of the feed would not even show
     * most of them, as replies open on request.
     * @param owner the page owner
     * @param viewer the signed-in user, whose own posts are not news
     * @return the largest such post id, 0 when there is none
     */
    long latestUserFeedActivity(String owner, String viewer);

    /**
     * The newest activity by others in a group's feed: new posts only.
     * @param groupId the group id
     * @param viewer the signed-in user, whose own posts are not news
     * @return the largest such post id, 0 when there is none
     */
    long latestGroupFeedActivity(Long groupId, String viewer);

    /**
     * Looks up a single publication or comment by id.
     * @param id the publication id
     * @return the publication, or null if none exists
     */
    Publication findById(Long id);
}

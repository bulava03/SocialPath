package com.socialpath.service;

import com.socialpath.entity.Publication;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.PublicationPresentable;

import java.io.IOException;
import java.util.List;

/**
 * The publication/comment feed. Publications and comments share one
 * self-referencing table (a {@link Publication} with an optional parent), so
 * the same operations serve both a top-level post and a nested comment.
 */
public interface CommentsService {

    /**
     * Creates a publication on a user's page.
     * @param newPublication the publication content and target
     */
    void addNewUserPublication(NewPublication newPublication) throws IOException;

    /**
     * Creates a publication on a group's page.
     * @param newPublication the publication content and target
     */
    void addNewGroupPublication(NewPublication newPublication) throws IOException;

    /**
     * Loads the feed of a user page with nested comments, oldest first.
     * @param login the page owner
     * @return the publications as view models
     */
    List<PublicationPresentable> loadUserFeed(String login);

    /**
     * Loads the feed of a group page with nested comments, oldest first.
     * @param groupId the group id
     * @return the publications as view models
     */
    List<PublicationPresentable> loadGroupFeed(Long groupId);

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

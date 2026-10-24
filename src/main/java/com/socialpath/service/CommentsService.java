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
     * Loads the presentable feed for a user or group page.
     * @param type "User" or "Group"
     * @param idInType the page owner's login or the group id
     * @return the page's publications, newest handling as stored
     */
    List<PublicationPresentable> loadComments(String type, String idInType) throws IOException;

    /**
     * Loads a set of publications by id into presentable form.
     * @param commentIds the publication ids to load
     * @return the publications as view models
     */
    List<PublicationPresentable> loadComments(List<Long> commentIds) throws IOException;

    /**
     * Returns the ids of a user's top-level publications.
     * @param login the page owner
     * @return the publication ids
     */
    List<Long> getCommentsIdsUser(String login);

    /**
     * Returns the ids of a group's top-level publications.
     * @param groupId the group id
     * @return the publication ids
     */
    List<Long> getCommentsIdsGroup(Long groupId);

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
     * Resolves publication ids into their presentable form, recursively
     * including nested comments.
     * @param ids the publication ids
     * @return the publications as view models, or null if ids is null
     */
    List<PublicationPresentable> getPublications(List<Long> ids) throws IOException;

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

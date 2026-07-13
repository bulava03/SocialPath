package com.socialpath.service;

import com.socialpath.document.Publication;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.PublicationPresentable;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.List;

/**
 * The publication/comment feed. Publications and comments share one recursive
 * document model (a {@link Publication} referencing child publications), so
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
    List<PublicationPresentable> loadComments(List<ObjectId> commentIds) throws IOException;

    /**
     * Returns the ids of a user's top-level publications.
     * @param login the page owner
     * @return the publication ids
     */
    List<ObjectId> getCommentsIdsUser(String login);

    /**
     * Returns the ids of a group's top-level publications.
     * @param groupId the group id
     * @return the publication ids
     */
    List<ObjectId> getCommentsIdsGroup(ObjectId groupId);

    /**
     * Looks up a single publication or comment by id.
     * @param id the publication id
     * @return the publication, or null if none exists
     */
    Publication findById(ObjectId id);

    /**
     * Resolves publication ids into their presentable form, recursively
     * including nested comments.
     * @param ids the publication ids
     * @return the publications as view models, or null if ids is null
     */
    List<PublicationPresentable> getPublications(List<ObjectId> ids) throws IOException;

    /**
     * Adds a comment under an existing publication.
     * @param newComment the comment content and parent
     */
    void addNewComment(NewComment newComment) throws IOException;

    /**
     * Deletes a top-level publication from a user's page, together with its
     * whole comment subtree and all associated media files.
     * @param delComment identifies the publication and its owner
     */
    void removePublicationUser(DelComment delComment);

    /**
     * Deletes a top-level publication from a group's page, together with its
     * whole comment subtree and all associated media files.
     * @param delComment identifies the publication and its group
     */
    void removePublicationGroup(DelComment delComment);

    /**
     * Deletes a comment, together with its whole reply subtree and all
     * associated media files.
     * @param delComment identifies the comment and its parent publication
     */
    void removeComment(DelComment delComment);
}

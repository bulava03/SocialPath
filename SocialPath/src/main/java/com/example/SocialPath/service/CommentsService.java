package com.example.SocialPath.service;

import com.example.SocialPath.document.Publication;
import com.example.SocialPath.extraClasses.DelComment;
import com.example.SocialPath.extraClasses.NewComment;
import com.example.SocialPath.extraClasses.NewPublication;
import com.example.SocialPath.extraClasses.PublicationPresentable;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.List;

public interface CommentsService {
    void addNewUserPublication(NewPublication newPublication) throws IOException;

    void addNewGroupPublication(NewPublication newPublication) throws IOException;

    List<PublicationPresentable> loadComments(String type, String idInType) throws IOException;
    List<PublicationPresentable> loadComments(List<ObjectId> commentIds) throws IOException;
    List<ObjectId> getCommentsIdsUser(String login);
    List<ObjectId> getCommentsIdsGroup(ObjectId groupId);

    Publication findById(ObjectId id);

    List<PublicationPresentable> getPublications(List<ObjectId> ids) throws IOException;

    void addNewComment(NewComment newComment) throws IOException;

    void removePublicationUser(DelComment delComment);

    void removePublicationGroup(DelComment delComment);

    void removeComment(DelComment delComment);
}

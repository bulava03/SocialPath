package com.example.SocialPath.service;

import com.example.SocialPath.document.Publication;
import com.example.SocialPath.extraClasses.DelComment;
import com.example.SocialPath.extraClasses.NewComment;
import com.example.SocialPath.extraClasses.NewPublication;
import com.example.SocialPath.extraClasses.PublicationPresentable;
import org.bson.types.ObjectId;

import java.util.List;

public interface CommentsService {
    void addNewUserPublication(NewPublication newPublication);

    void addNewGroupPublication(NewPublication newPublication);

    List<PublicationPresentable> loadComments(String type, String idInType);

    Publication findById(ObjectId id);

    List<PublicationPresentable> getPublications(List<ObjectId> ids);

    void addNewComment(NewComment newComment);

    void removePublicationUser(DelComment delComment);

    void removePublicationGroup(DelComment delComment);

    void removeComment(DelComment delComment);
}

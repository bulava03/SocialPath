package com.example.SocialPath_Admin.service;

import com.example.SocialPath_Admin.document.Publication;
import com.example.SocialPath_Admin.extraClasses.PublicationPresentable;
import org.bson.types.ObjectId;

import java.util.List;

public interface CommentsService {
    Publication findById(ObjectId id);
    List<PublicationPresentable> loadComments(ObjectId groupId);
    List<PublicationPresentable> getPublications(List<ObjectId> ids);
    void removePublicationUser(String login, ObjectId idComment);
    void removePublicationGroup(ObjectId groupId, ObjectId idComment);
    void removeCommentUser(ObjectId idPublication, ObjectId idComment);
    void removeCommentGroup(ObjectId idPublication, ObjectId idComment);
    List<ObjectId> findByAuthorId(String authorId);
    void removeAllByIds(List<ObjectId> ids);
}

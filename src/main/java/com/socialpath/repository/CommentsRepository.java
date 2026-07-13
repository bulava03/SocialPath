package com.socialpath.repository;

import com.socialpath.document.Publication;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface CommentsRepository extends MongoRepository<Publication, ObjectId> {

    @Transactional
    default void pushCommentId(ObjectId commentId, ObjectId publicationId) {
        Publication publication = findById(publicationId).orElse(null);
        if (publication != null) {
            List<ObjectId> comments = publication.getComments() == null ? new ArrayList<>() : publication.getComments();
            comments.add(commentId);
            publication.setComments(comments);
            save(publication);
        }
    }

    @Transactional
    default void removeCommentFromPublication(ObjectId publicationId, ObjectId commentId) {
        Publication publication = findById(publicationId).orElse(null);
        if (publication != null) {
            List<ObjectId> ids = publication.getComments();
            ids.remove(commentId);
            publication.setComments(ids);
            save(publication);
        }
    }

}

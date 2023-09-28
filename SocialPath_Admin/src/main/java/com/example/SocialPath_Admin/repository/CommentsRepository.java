package com.example.SocialPath_Admin.repository;

import com.example.SocialPath_Admin.document.Publication;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface CommentsRepository extends MongoRepository<Publication, ObjectId> {

    List<ObjectId> findByAuthorId(String authorId);

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

    default List<ObjectId> getPublications(ObjectId id) {
        List<ObjectId> list = new ArrayList<>();
        list.add(id);
        Publication publication = findById(id).orElse(null);
        if (publication != null && publication.getComments() != null) {
            for (ObjectId commentId : publication.getComments()) {
                List<ObjectId> subList = getPublications(commentId);
                list.addAll(subList);
            }
        }
        return list;
    }

    @Transactional
    default void removePublication(ObjectId id) {
        List<ObjectId> publicationIds = getPublications(id);
        deleteAllById(publicationIds);
    }

}

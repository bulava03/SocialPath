package com.example.SocialPath_Admin.repository;

import com.example.SocialPath_Admin.document.Group;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, ObjectId> {

    List<Group> findAllByOwner(String owner);

    @Transactional
    default void removeFromAdmins(ObjectId groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> admins = group.getAdmins();
            admins.remove(userId);
            group.setAdmins(admins);
            save(group);
        }
    }

    @Transactional
    default void removeFromGroup(ObjectId groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> logins = group.getMembers();
            logins.remove(userId);
            group.setMembers(logins);
            save(group);
        }
    }

    @Transactional
    default void removePublicationFromGroup(ObjectId groupId, ObjectId publicationId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<ObjectId> publications = group.getPublications();
            publications.remove(publicationId);
            group.setPublications(publications);
            save(group);
        }
    }

}

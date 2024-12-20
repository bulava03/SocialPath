package com.example.SocialPath.repository;

import com.example.SocialPath.document.Group;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public interface GroupRepository extends MongoRepository<Group, ObjectId> {
    List<Group> findByNameIn(List<String> searchValues);

    default List<String> findMembersById(ObjectId id) {
        Group group = findById(id).orElse(null);
        if (group != null) {
            return group.getMembers();
        }
        return null;
    }

    default List<String> findAdminsById(ObjectId id) {
        Group group = findById(id).orElse(null);
        if (group != null) {
            return group.getAdmins();
        }
        return null;
    }

    default String findOwnerById(ObjectId id) {
        Group group = findById(id).orElse(null);
        if (group != null) {
            return group.getOwner();
        }
        return null;
    }

    default void addAdmin(ObjectId groupId, String newAdminId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> admins = group.getAdmins();
            admins.add(newAdminId);
            group.setAdmins(admins);
            save(group);
        }
    }

    default void addUserToGroup(ObjectId groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> logins = group.getMembers();
            logins.add(userId);
            group.setMembers(logins);
            save(group);
        }
    }

    default void removeFromAdmins(ObjectId groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> admins = group.getAdmins();
            admins.remove(userId);
            group.setAdmins(admins);
            save(group);
        }
    }

    default void removeFromGroup(ObjectId groupId, String userId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<String> logins = group.getMembers();
            logins.remove(userId);
            group.setMembers(logins);
            save(group);
        }
    }

    default void addPublicationToGroup(ObjectId publicationId, ObjectId groupId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<ObjectId> publicationsUpdated = group.getPublications() == null ? new ArrayList<>() : group.getPublications();
            publicationsUpdated.add(publicationId);
            group.setPublications(publicationsUpdated);
            save(group);
        }
    }

    default void removePublicationFromGroup(ObjectId groupId, ObjectId publicationId) {
        Group group = findById(groupId).orElse(null);
        if (group != null) {
            List<ObjectId> publications = group.getPublications();
            publications.remove(publicationId);
            group.setPublications(publications);
            save(group);
        }
    }

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'name': ?1, 'imageId': ?2 }}")
    void updateGroup(ObjectId id, String name, String imageId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$push': { 'members': ?1 }}")
    void addMember(ObjectId groupId, String memberId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$pull': { 'members': ?1 }}")
    void removeMember(ObjectId groupId, String memberId);
}

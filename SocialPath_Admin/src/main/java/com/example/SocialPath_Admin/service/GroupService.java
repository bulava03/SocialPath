package com.example.SocialPath_Admin.service;

import com.example.SocialPath_Admin.document.Group;
import org.bson.types.ObjectId;

import java.util.List;

public interface GroupService {
    Group findById(ObjectId id);
    List<String> findGroupsAdmins(ObjectId id);
    String findGroupOwner(ObjectId id);
    void removeFromAdmins(ObjectId groupId, String userId);
    void removeFromGroup(ObjectId groupId, String userId);
    void removeUserFromGroup(ObjectId groupId, String userId);
    void removeGroupById(ObjectId groupId);
    void removePublicationIdsFromAllGroups(List<ObjectId> publicationIds);
    void removeObjectFromGroupArrays(String objectId);
    List<ObjectId> findGroupIdsByOwner(String owner);
}

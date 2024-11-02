package com.example.SocialPath.service;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.extraClasses.GroupCreationForm;
import com.example.SocialPath.extraClasses.UserSearchResult;
import org.bson.types.ObjectId;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface GroupService {

    Object[] validateGroup(GroupCreationForm group);

    Group addGroup(Group group);

    Group findGroupById(ObjectId id);

    List<UserSearchResult> findGroupsMembers(ObjectId id) throws IOException;

    List<UserSearchResult> getAdminsObjList(List<String> admins);

    List<String> findGroupsAdmins(ObjectId id);

    List<UserSearchResult> findGroupsAdminsPresentable(ObjectId id) throws IOException;

    String findGroupOwner(ObjectId id);

    void joinGroup(ObjectId groupId, String userId);

    void removeFromAdmins(ObjectId groupId, String userId);

    void removeFromGroup(ObjectId groupId, String userId);

    void addToAdmins(ObjectId groupId, String userId);
    void removeUserFromGroup(ObjectId groupId, String userId);

    void updateGroup(Group groupUpdated, MultipartFile file) throws IOException;

    void addMember(String groupId, String memberId);
    void removeMember(String groupId, String memberId);
}

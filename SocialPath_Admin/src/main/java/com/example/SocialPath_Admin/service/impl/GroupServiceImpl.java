package com.example.SocialPath_Admin.service.impl;

import com.example.SocialPath_Admin.document.Group;
import com.example.SocialPath_Admin.repository.GroupRepository;
import com.example.SocialPath_Admin.repository.UserRepository;
import com.example.SocialPath_Admin.service.GroupService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;

    @Override
    public Group findById(ObjectId id) {
        return groupRepository.findById(id).orElse(null);
    }

    @Override
    public List<String> findGroupsAdmins(ObjectId id) {
        Optional<Group> group = groupRepository.findById(id);
        return group.map(Group::getAdmins).orElse(null);
    }

    @Override
    public String findGroupOwner(ObjectId id) {
        Optional<Group> group = groupRepository.findById(id);
        return group.map(Group::getOwner).orElse(null);
    }

    @Override
    public void removeFromAdmins(ObjectId groupId, String userId) {
        groupRepository.removeFromAdmins(groupId, userId);
    }

    @Override
    public void removeFromGroup(ObjectId groupId, String userId) {
        userRepository.removeFromGroups(userId, groupId);
        groupRepository.removeFromGroup(groupId, userId);
    }

    @Override
    public void removeUserFromGroup(ObjectId groupId, String userId) {
        removeFromAdmins(groupId, userId);
        removeFromGroup(groupId, userId);
    }

    @Override
    public void removeGroupById(ObjectId groupId) {
        groupRepository.deleteById(groupId);
    }

    @Override
    public void removePublicationIdsFromAllGroups(List<ObjectId> publicationIds) {
        List<Group> groupsFound = groupRepository.findAll();
        List<Group> groups = new ArrayList<>();
        for (Group group : groupsFound
             ) {
            if (group.getPublications() != null) {
                List<ObjectId> publications = group.getPublications();
                publications.removeAll(publicationIds);
                group.setPublications(publications);
                groups.add(group);
            }
        }
        groupRepository.saveAll(groups);
    }

    @Override
    public void removeObjectFromGroupArrays(String userLogin) {
        List<Group> groups = groupRepository.findAll();
        for (Group group : groups
        ) {
            if (group.getMembers() != null) {
                List<String> members = group.getMembers();
                members.remove(userLogin);
                group.setMembers(members);
            }
            if (group.getAdmins() != null) {
                List<String> admins = group.getAdmins();
                admins.remove(userLogin);
                group.setAdmins(admins);
            }
        }
        groupRepository.saveAll(groups);
    }

    @Override
    public List<ObjectId> findGroupIdsByOwner(String owner) {
        List<Group> groups = groupRepository.findAllByOwner(owner);
        List<ObjectId> list = new ArrayList<>();
        for (Group group : groups
             ) {
            list.add(group.getId());
        }
        return list;
    }

}

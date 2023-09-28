package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupCreationForm;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.GroupService;
import com.example.SocialPath.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GroupServiceImpl implements GroupService {
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Validator validator;

    @Override
    public Object[] validateGroup(GroupCreationForm group) {
        Set<ConstraintViolation<GroupCreationForm>> violations = validator.validate(group);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else {
            return new Object[] { true, "" };
        }
    }

    @Override
    public Group addGroup(Group group) {
        group.setMembers(new ArrayList<>(Collections.singletonList(group.getOwner())));
        group.setAdmins(new ArrayList<>(Collections.singletonList(group.getOwner())));
        Group savedGroup = groupRepository.save(group);
        ObjectId groupId = savedGroup.getId();
        userRepository.addGroupToUser(groupId, group.getOwner());
        return savedGroup;
    }

    @Override
    public Group findGroupById(ObjectId id) {
        return groupRepository.findById(id).orElse(null);
    }

    @Override
    public List<UserSearchResult> findGroupsMembers(ObjectId id) {
        List<String> groupsMembers = groupRepository.findMembersById(id);
        List<User> membersObj = userRepository.findByLoginIn(groupsMembers);
        List<UserSearchResult> groupsMembersPresentable = new ArrayList<>();
        for (User user : membersObj
             ) {
            UserSearchResult toAdd = modelMapper.map(user, UserSearchResult.class);
            toAdd.setAnotherUserLogin(user.getLogin());
            groupsMembersPresentable.add(toAdd);
        }
        return groupsMembersPresentable;
    }

    @Override
    public List<UserSearchResult> getAdminsObjList(List<String> admins) {
        List<User> adminsObj = userRepository.findByLoginIn(admins);
        List<UserSearchResult> usersAdminsPresentable = new ArrayList<>();
        for (User user : adminsObj
        ) {
            UserSearchResult toAdd = modelMapper.map(user, UserSearchResult.class);
            toAdd.setAnotherUserLogin(user.getLogin());
            usersAdminsPresentable.add(toAdd);
        }
        return usersAdminsPresentable;
    }

    @Override
    public List<String> findGroupsAdmins(ObjectId id) {
        return groupRepository.findAdminsById(id);
    }

    @Override
    public List<UserSearchResult> findGroupsAdminsPresentable(ObjectId id) {
        List<String> groupsAdmins = groupRepository.findAdminsById(id);
        List<User> adminsObj = userRepository.findByLoginIn(groupsAdmins);
        List<UserSearchResult> groupsAdminsPresentable = new ArrayList<>();
        for (User user : adminsObj
        ) {
            UserSearchResult toAdd = modelMapper.map(user, UserSearchResult.class);
            toAdd.setAnotherUserLogin(user.getLogin());
            groupsAdminsPresentable.add(toAdd);
        }
        return groupsAdminsPresentable;
    }

    @Override
    public String findGroupOwner(ObjectId id) {
        return groupRepository.findOwnerById(id);
    }

    @Override
    public void joinGroup(ObjectId groupId, String userId) {
        userRepository.addGroupToUser(groupId, userId);
        groupRepository.addUserToGroup(groupId, userId);
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
    public void addToAdmins(ObjectId groupId, String userId) {
        groupRepository.addAdmin(groupId, userId);
    }

    @Override
    public void removeUserFromGroup(ObjectId groupId, String userId) {
        removeFromAdmins(groupId, userId);
        removeFromGroup(groupId, userId);
    }
}

package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.Group;
import com.socialpath.document.User;
import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.validation.ValidationResult;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.GroupService;
import com.socialpath.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final HandleAvatarService handleAvatarService;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final Validator validator;

    @Override
    public ValidationResult validateGroup(GroupCreationForm group) {
        Set<ConstraintViolation<GroupCreationForm>> violations = validator.validate(group);

        if (!violations.isEmpty()) {
            return ValidationResult.failure(violations.stream()
                    .map(ConstraintViolation::getMessage).toList());
        }
        return ValidationResult.success();
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
    public List<UserSearchResult> findGroupsMembers(ObjectId id) throws IOException {
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
    public List<String> findGroupsAdmins(ObjectId id) {
        return groupRepository.findAdminsById(id);
    }

    @Override
    public List<UserSearchResult> findGroupsAdminsPresentable(ObjectId id) throws IOException {
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
    public void removeFromAdmins(ObjectId groupId, String userId) {
        groupRepository.removeFromAdmins(groupId, userId);
    }

    private void removeFromGroup(ObjectId groupId, String userId) {
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

    @Override
    public void updateGroup(Group groupUpdated, MultipartFile file, boolean removeImage) throws IOException {
        Group group = findGroupById(groupUpdated.getId());
        group.setName(groupUpdated.getName());
        group.setImageId(handleAvatarService.updateGroupAvatar(group, file, removeImage));

        groupRepository.updateGroup(group.getId(), group.getName(), group.getImageId());
    }

    @Override
    public void addMember(String groupId, String memberId) {
        groupRepository.addMember(new ObjectId(groupId), memberId);
    }

    @Override
    public void removeMember(String groupId, String memberId) {
        groupRepository.removeMember(new ObjectId(groupId), memberId);
    }
}

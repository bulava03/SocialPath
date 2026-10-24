package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Group;
import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.validation.ValidationResult;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.GroupService;
import jakarta.validation.Validator;
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
    private final Validator validator;

    @Override
    public ValidationResult validateGroup(GroupCreationForm group) {
        return ValidationResult.of(validator.validate(group));
    }

    @Override
    public Group addGroup(Group group) {
        // The owner starts as the sole member and admin. Membership now lives
        // only in group_members/group_admins, so there is no user-side list to
        // keep in sync (the document model synchronized two arrays here).
        group.setMembers(new ArrayList<>(Collections.singletonList(group.getOwner())));
        group.setAdmins(new ArrayList<>(Collections.singletonList(group.getOwner())));
        return groupRepository.save(group);
    }

    @Override
    public Group findGroupById(Long id) {
        return groupRepository.findById(id).orElse(null);
    }

    @Override
    public Group getGroup(String id) {
        Group group = null;
        try {
            group = findGroupById(Long.valueOf(id));
        } catch (NumberFormatException e) {
            // Falls through to "not found": a malformed id names no group.
        }
        if (group == null) {
            throw new ResourceNotFoundException("Group not found: " + id);
        }
        return group;
    }

    @Override
    public List<UserSearchResult> findGroupsMembers(Long id) throws IOException {
        return toSearchResults(groupRepository.findMembersById(id));
    }

    @Override
    public List<String> findGroupsAdmins(Long id) {
        return groupRepository.findAdminsById(id);
    }

    @Override
    public List<UserSearchResult> findGroupsAdminsPresentable(Long id) throws IOException {
        return toSearchResults(groupRepository.findAdminsById(id));
    }

    private List<UserSearchResult> toSearchResults(List<String> logins) {
        return userRepository.findByLoginIn(logins).stream()
                .map(UserSearchResult::from)
                .toList();
    }

    @Override
    public void removeFromAdmins(Long groupId, String userId) {
        groupRepository.removeFromAdmins(groupId, userId);
    }

    @Override
    public void addToAdmins(Long groupId, String userId) {
        groupRepository.addAdmin(groupId, userId);
    }

    @Override
    public void removeUserFromGroup(Long groupId, String userId) {
        groupRepository.removeFromAdmins(groupId, userId);
        groupRepository.removeMember(groupId, userId);
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
        groupRepository.addMember(Long.valueOf(groupId), memberId);
    }

    @Override
    public void removeMember(String groupId, String memberId) {
        groupRepository.removeMember(Long.valueOf(groupId), memberId);
    }
}

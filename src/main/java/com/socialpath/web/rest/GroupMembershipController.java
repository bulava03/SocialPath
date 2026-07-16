package com.socialpath.web.rest;

import com.socialpath.entity.Group;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Join/leave endpoints. Membership has a single source of truth now (the
 * group_members table), so unlike the document edition there is no user-side
 * membership list to keep in sync.
 */
@RestController
@RequestMapping("/groupMembership")
@RequiredArgsConstructor
public class GroupMembershipController {

    private final GroupService groupService;

    @PostMapping("/joinGroup")
    public OperationResult joinGroup(String groupId) {
        String authorLogin = SecurityUtils.getCurrentLogin();
        Group group = requireGroup(groupId);

        if (group.getMembers() != null && group.getMembers().contains(authorLogin)) {
            return OperationResult.of("ALREADY_MEMBER");
        }

        groupService.addMember(groupId, authorLogin);

        return OperationResult.of("JOINED");
    }

    @PostMapping("/leaveGroup")
    public OperationResult leaveGroup(String groupId) {
        String authorLogin = SecurityUtils.getCurrentLogin();
        Group group = requireGroup(groupId);

        if (group.getMembers() != null && !group.getMembers().contains(authorLogin)) {
            return OperationResult.of("NOT_A_MEMBER");
        }

        groupService.removeMember(groupId, authorLogin);

        return OperationResult.of("LEFT");
    }

    private Group requireGroup(String groupId) {
        Group group = groupService.findGroupById(Long.valueOf(groupId));
        if (group == null) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }
        return group;
    }
}

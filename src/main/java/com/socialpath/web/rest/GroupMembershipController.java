package com.socialpath.web.rest;

import com.socialpath.document.Group;
import com.socialpath.document.User;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.security.SecurityUtils;
import com.socialpath.service.GroupService;
import com.socialpath.service.UserService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groupMembership")
@RequiredArgsConstructor
public class GroupMembershipController {

    private final UserService userService;
    private final GroupService groupService;

    @PostMapping("/joinGroup")
    public OperationResult joinGroup(String groupId) {
        String authorLogin = SecurityUtils.getCurrentLogin();
        User myUser = userService.findByLogin(authorLogin);
        Group group = requireGroup(groupId);

        if (group.getMembers() != null && group.getMembers().contains(authorLogin)) {
            return OperationResult.of("ALREADY_MEMBER");
        }

        groupService.addMember(groupId, authorLogin);

        if (myUser.getGroups() == null || !myUser.getGroups().contains(new ObjectId(groupId))) {
            userService.addGroup(authorLogin, groupId);
        }

        return OperationResult.of("JOINED");
    }

    @PostMapping("/leaveGroup")
    public OperationResult leaveGroup(String groupId) {
        String authorLogin = SecurityUtils.getCurrentLogin();
        User myUser = userService.findByLogin(authorLogin);
        Group group = requireGroup(groupId);

        if (group.getMembers() != null && !group.getMembers().contains(authorLogin)) {
            return OperationResult.of("NOT_A_MEMBER");
        }

        groupService.removeMember(groupId, authorLogin);

        if (myUser.getGroups() != null && myUser.getGroups().contains(new ObjectId(groupId))) {
            userService.removeGroup(authorLogin, groupId);
        }

        return OperationResult.of("LEFT");
    }

    private Group requireGroup(String groupId) {
        Group group = groupService.findGroupById(new ObjectId(groupId));
        if (group == null) {
            throw new ResourceNotFoundException("Group not found: " + groupId);
        }
        return group;
    }
}

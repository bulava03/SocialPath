package com.socialpath.web.rest;

import com.socialpath.entity.Group;
import com.socialpath.dto.response.OperationResult;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.entity.User;
import com.socialpath.security.CurrentUser;
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
    public OperationResult joinGroup(@CurrentUser User me, String groupId) {
        String authorLogin = me.getLogin();
        Group group = groupService.getGroup(groupId);

        if (group.isMember(authorLogin)) {
            return OperationResult.of("ALREADY_MEMBER");
        }

        groupService.addMember(groupId, authorLogin);

        return OperationResult.of("JOINED");
    }

    @PostMapping("/leaveGroup")
    public OperationResult leaveGroup(@CurrentUser User me, String groupId) {
        String authorLogin = me.getLogin();
        Group group = groupService.getGroup(groupId);

        if (!group.isMember(authorLogin)) {
            return OperationResult.of("NOT_A_MEMBER");
        }
        // The owner is the group's permanent member and admin.
        if (group.isOwner(authorLogin)) {
            throw new ForbiddenOperationException("The group owner cannot leave the group");
        }

        groupService.removeMember(groupId, authorLogin);

        return OperationResult.of("LEFT");
    }
}

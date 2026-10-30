package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.dto.request.GroupUserRequest;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.service.GroupService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.web.GroupController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.ui.ExtendedModelMap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupControllerAuthorizationTest {

    private GroupService groupService;
    private GroupController controller;

    private final Long groupId = 20L;
    private Group group;

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        controller = new GroupController(
                groupService,
                mock(ModelMapper.class),
                mock(ModelAttributesService.class),
                mock(HandleAvatarService.class));

        group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(new ArrayList<>(List.of("owner", "admin")));
        group.setMembers(new ArrayList<>(List.of("owner", "admin", "member", "member2")));
        when(groupService.getGroup(groupId.toString())).thenReturn(group);
    }

    private User user(String login) {
        User user = new User();
        user.setLogin(login);
        return user;
    }

    private GroupUserRequest request(String targetUser) {
        GroupUserRequest request = new GroupUserRequest();
        request.setGroupId(groupId.toString());
        request.setUserId(targetUser);
        return request;
    }

    @Test
    void removeUserFromGroup_OwnerRemovesMember_Succeeds() {
        controller.removeUserFromGroup(user("owner"), request("member"));

        verify(groupService).removeUserFromGroup(groupId, "member");
    }

    @Test
    void removeUserFromGroup_AdminRemovesPlainMember_Succeeds() {
        controller.removeUserFromGroup(user("admin"), request("member"));

        verify(groupService).removeUserFromGroup(groupId, "member");
    }

    @Test
    void removeUserFromGroup_MemberCannotRemoveAnyone() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(user("member"), request("member2")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void removeUserFromGroup_AdminCannotRemoveAnotherAdmin() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(user("admin"), request("owner")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void removeUserFromGroup_NobodyCanRemoveOwner() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(user("owner"), request("owner")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void addToAdmins_NonOwnerForbidden() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addToAdmins(user("admin"), request("member")));
        verify(groupService, never()).addToAdmins(any(), any());
    }

    @Test
    void addToAdmins_OwnerSucceeds() {
        controller.addToAdmins(user("owner"), request("member"));

        verify(groupService).addToAdmins(groupId, "member");
    }

    @Test
    void addToAdmins_NonMemberCannotBecomeAdmin() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.addToAdmins(user("owner"), request("stranger")));
        verify(groupService, never()).addToAdmins(any(), any());
    }

    @Test
    void removeFromAdmins_OwnerCannotBeDemoted() {
        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeFromAdmins(user("owner"), request("owner")));
        verify(groupService, never()).removeFromAdmins(any(), any());
    }

    @Test
    void removeFromAdmins_OwnerDemotesAdmin_Succeeds() {
        controller.removeFromAdmins(user("owner"), request("admin"));
        verify(groupService).removeFromAdmins(groupId, "admin");
    }

    @Test
    void removeFromAdmins_AdminCannotDemoteAnotherAdmin() {
        group.getAdmins().add("member");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeFromAdmins(user("admin"), request("member")));
        verify(groupService, never()).removeFromAdmins(any(), any());
    }

    @Test
    void updateGroup_NonOwnerCannotChangeTheGroup() throws Exception {
        Group renamed = new Group();
        renamed.setName("Taken over");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.updateGroup(user("admin"), renamed, groupId.toString(), null, false,
                        new ExtendedModelMap()));
        verify(groupService, never()).updateGroup(any(), any(), anyBoolean());
    }
}

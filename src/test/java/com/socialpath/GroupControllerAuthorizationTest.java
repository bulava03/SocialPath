package com.socialpath;

import com.socialpath.document.Group;
import com.socialpath.dto.request.GroupUserRequest;
import com.socialpath.exception.ForbiddenOperationException;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.service.GroupService;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.UserService;
import com.socialpath.web.GroupController;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupControllerAuthorizationTest {

    private GroupService groupService;
    private GroupController controller;

    private final ObjectId groupId = new ObjectId();
    private Group group;

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        controller = new GroupController(
                groupService,
                mock(UserService.class),
                mock(ModelMapper.class),
                mock(ModelAttributesService.class),
                mock(HandleAvatarService.class));

        group = new Group();
        group.setId(groupId);
        group.setOwner("owner");
        group.setAdmins(new ArrayList<>(List.of("owner", "admin")));
        group.setMembers(new ArrayList<>(List.of("owner", "admin", "member", "member2")));
        when(groupService.findGroupById(groupId)).thenReturn(group);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String login) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }

    private GroupUserRequest request(String targetUser) {
        GroupUserRequest request = new GroupUserRequest();
        request.setGroupId(groupId.toString());
        request.setUserId(targetUser);
        return request;
    }

    @Test
    void removeUserFromGroup_OwnerRemovesMember_Succeeds() {
        loginAs("owner");

        controller.removeUserFromGroup(request("member"));

        verify(groupService).removeUserFromGroup(groupId, "member");
    }

    @Test
    void removeUserFromGroup_AdminRemovesPlainMember_Succeeds() {
        loginAs("admin");

        controller.removeUserFromGroup(request("member"));

        verify(groupService).removeUserFromGroup(groupId, "member");
    }

    @Test
    void removeUserFromGroup_MemberCannotRemoveAnyone() {
        loginAs("member");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(request("member2")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void removeUserFromGroup_AdminCannotRemoveAnotherAdmin() {
        loginAs("admin");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(request("owner")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void removeUserFromGroup_NobodyCanRemoveOwner() {
        loginAs("owner");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeUserFromGroup(request("owner")));
        verify(groupService, never()).removeUserFromGroup(any(), any());
    }

    @Test
    void addToAdmins_NonOwnerForbidden() {
        loginAs("admin");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.addToAdmins(request("member")));
        verify(groupService, never()).addToAdmins(any(), any());
    }

    @Test
    void addToAdmins_OwnerSucceeds() {
        loginAs("owner");

        controller.addToAdmins(request("member"));

        verify(groupService).addToAdmins(groupId, "member");
    }

    @Test
    void removeFromAdmins_OwnerCannotBeDemoted() {
        loginAs("owner");

        assertThrows(ForbiddenOperationException.class,
                () -> controller.removeFromAdmins(request("owner")));
        verify(groupService, never()).removeFromAdmins(any(), any());
    }

    @Test
    void removeUserFromGroup_UnknownGroup_NotFound() {
        loginAs("owner");
        ObjectId missing = new ObjectId();
        when(groupService.findGroupById(missing)).thenReturn(null);

        GroupUserRequest request = request("member");
        request.setGroupId(missing.toString());

        assertThrows(ResourceNotFoundException.class,
                () -> controller.removeUserFromGroup(request));
    }
}

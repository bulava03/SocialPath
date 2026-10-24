package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.HandleAvatarService;
import com.socialpath.service.impl.GroupServiceImpl;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupLookupTest {

    private GroupRepository groupRepository;
    private GroupServiceImpl service;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        service = new GroupServiceImpl(
                mock(HandleAvatarService.class),
                groupRepository,
                mock(UserRepository.class),
                mock(Validator.class));
    }

    @Test
    void getGroup_Existing_ReturnsIt() {
        Group group = new Group();
        when(groupRepository.findById(5L)).thenReturn(Optional.of(group));

        assertSame(group, service.getGroup("5"));
    }

    @Test
    void getGroup_MissingOrMalformedId_NotFound() {
        when(groupRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getGroup("5"));
        assertThrows(ResourceNotFoundException.class, () -> service.getGroup("abc"));
        assertThrows(ResourceNotFoundException.class, () -> service.getGroup(null));
    }

    @Test
    void roles_OwnerIsModeratorAndPlainMemberIsNot() {
        Group group = new Group();
        group.setOwner("owner");
        group.setAdmins(List.of("owner"));
        group.setMembers(List.of("owner", "member"));

        assertTrue(group.isModerator("owner"));
        assertTrue(group.isMember("member"));
        assertFalse(group.isModerator("member"));
        assertFalse(group.isMember("stranger"));
    }

    @Test
    void roles_WithoutLists_NobodyIsMember() {
        Group group = new Group();

        assertFalse(group.isMember("anyone"));
        assertFalse(group.isAdmin("anyone"));
        assertFalse(group.isOwner("anyone"));
    }
}

package com.socialpath;

import com.socialpath.dto.request.GroupCreationForm;
import com.socialpath.dto.request.LeftFrameRequest;
import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.media.AvatarImageProcessor;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.GroupService;
import com.socialpath.service.ModelAttributesService;
import com.socialpath.service.impl.HandleAvatarServiceImpl;
import com.socialpath.validation.ValidationResult;
import com.socialpath.web.GroupController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Every group page shows the viewer's own avatar in the header (layout2
 * reads the "avatar" attribute); without it the header falls back to the
 * default picture.
 */
class GroupControllerHeaderAvatarTest {

    private static final String AVATAR = "3f1c2a9e-0b7d-4c55-9a51-6a2f7c1d8e40.jpg";

    private GroupService groupService;
    private GroupController controller;
    private final User viewer = new User();
    private final Model model = new ExtendedModelMap();
    private final LeftFrameRequest request = new LeftFrameRequest();

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        ModelAttributesService modelAttributes = mock(ModelAttributesService.class);
        when(modelAttributes.groupsAttributes(any(), any(), any(), any(), any()))
                .thenAnswer(call -> call.getArgument(0));
        controller = new GroupController(groupService, mock(ModelMapper.class), modelAttributes,
                new HandleAvatarServiceImpl(mock(FileStorageService.class), mock(AvatarImageProcessor.class)));

        viewer.setLogin("viewer");
        viewer.setImageId(AVATAR);

        Group group = new Group();
        group.setId(20L);
        group.setOwner("owner");
        when(groupService.getGroup("20")).thenReturn(group);
        request.setId("20");
    }

    @Test
    void groupCreationForm_ShowsTheViewersAvatar() {
        controller.getGroupCreationForm(viewer, model);

        assertEquals(AVATAR, model.getAttribute("avatar"));
    }

    @Test
    void groupCreationForm_RejectedName_StillShowsTheViewersAvatar() {
        when(groupService.validateGroup(any())).thenReturn(ValidationResult.failure("Name is required"));

        controller.createGroup(viewer, new GroupCreationForm("viewer"), model);

        assertEquals(AVATAR, model.getAttribute("avatar"));
    }

    @Test
    void membersPage_ShowsTheViewersAvatar() {
        when(groupService.findGroupsMembers(20L)).thenReturn(List.of());
        when(groupService.findGroupsAdmins(20L)).thenReturn(List.of());

        controller.getGroupsMembers(viewer, request, model);

        assertEquals(AVATAR, model.getAttribute("avatar"));
    }

    @Test
    void adminsPage_ShowsTheViewersAvatar() {
        when(groupService.findGroupsAdminsPresentable(20L)).thenReturn(List.of());

        controller.getGroupsAdmins(viewer, request, model);

        assertEquals(AVATAR, model.getAttribute("avatar"));
    }
}

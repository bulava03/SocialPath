package com.socialpath;

import com.socialpath.dto.request.GroupForm;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.function.Consumer;

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
    private final LeftFrameRequest request = new LeftFrameRequest();

    @BeforeEach
    void setUp() {
        groupService = mock(GroupService.class);
        ModelAttributesService modelAttributes = mock(ModelAttributesService.class);
        when(modelAttributes.groupsAttributes(any(), any(), any(), any(), any()))
                .thenAnswer(call -> call.getArgument(0));
        controller = new GroupController(groupService, modelAttributes,
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
    void everyGroupPage_ShowsTheViewersAvatarInTheHeader() {
        when(groupService.validateGroup(any())).thenReturn(ValidationResult.failure("Name is required"));
        when(groupService.findGroupsMembers(20L)).thenReturn(List.of());
        when(groupService.findGroupsAdmins(20L)).thenReturn(List.of());
        when(groupService.findGroupsAdminsPresentable(20L)).thenReturn(List.of());

        // The creation form, also when shown again after a rejected name, and both member lists.
        List<Consumer<Model>> pages = List.of(
                model -> controller.getGroupCreationForm(viewer, model),
                model -> controller.createGroup(viewer, new GroupForm(), model),
                model -> controller.getGroupsMembers(viewer, request, model),
                model -> controller.getGroupsAdmins(viewer, request, model));
        for (Consumer<Model> page : pages) {
            Model model = new ExtendedModelMap();
            page.accept(model);
            assertEquals(AVATAR, model.getAttribute("avatar"));
        }
    }
}

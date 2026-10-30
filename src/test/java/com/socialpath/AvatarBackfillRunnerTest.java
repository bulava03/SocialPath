package com.socialpath;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.maintenance.AvatarBackfillRunner;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.HandleAvatarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Old avatars are converted in place on startup. Whatever fails, a user must
 * never end up pointing at a file that no longer exists.
 */
class AvatarBackfillRunnerTest {

    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private HandleAvatarService handleAvatarService;
    private AvatarBackfillRunner runner;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        groupRepository = mock(GroupRepository.class);
        handleAvatarService = mock(HandleAvatarService.class);
        runner = new AvatarBackfillRunner(userRepository, groupRepository, handleAvatarService);
        when(groupRepository.findAll()).thenReturn(List.of());
    }

    private static User user(String login, String imageId) {
        User user = new User();
        user.setLogin(login);
        user.setImageId(imageId);
        return user;
    }

    @Test
    void oldAvatar_NewIdIsSavedBeforeTheOldFileIsDeleted() throws IOException {
        when(userRepository.findByImageIdIsNotNull()).thenReturn(List.of(user("anna", "old.png")));
        when(handleAvatarService.needsProcessing("old.png")).thenReturn(true);
        when(handleAvatarService.reprocessStoredAvatar("old.png")).thenReturn("new.jpg");

        runner.run(null);

        InOrder order = inOrder(handleAvatarService, userRepository);
        order.verify(handleAvatarService).reprocessStoredAvatar("old.png");
        order.verify(userRepository).updateImageId("anna", "new.jpg");
        order.verify(handleAvatarService).deleteAvatar("old.png");
    }

    @Test
    void processedAvatar_IsLeftAlone() throws IOException {
        when(userRepository.findByImageIdIsNotNull()).thenReturn(List.of(user("anna", "done.jpg")));
        when(handleAvatarService.needsProcessing("done.jpg")).thenReturn(false);

        runner.run(null);

        verify(handleAvatarService, never()).reprocessStoredAvatar(anyString());
        verify(userRepository, never()).updateImageId(anyString(), anyString());
    }

    @Test
    void unreadableAvatar_KeepsTheOldOne_AndTheOthersAreStillConverted() throws IOException {
        when(userRepository.findByImageIdIsNotNull())
                .thenReturn(List.of(user("anna", "broken.png"), user("ben", "old.png")));
        when(handleAvatarService.needsProcessing(anyString())).thenReturn(true);
        when(handleAvatarService.reprocessStoredAvatar("broken.png")).thenThrow(new IOException("unreadable"));
        when(handleAvatarService.reprocessStoredAvatar("old.png")).thenReturn("new.jpg");

        runner.run(null);

        verify(userRepository, never()).updateImageId("anna", "new.jpg");
        verify(handleAvatarService, never()).deleteAvatar("broken.png");
        verify(userRepository).updateImageId("ben", "new.jpg");
    }

    @Test
    void failedSave_DoesNotDeleteTheOldFile() throws IOException {
        when(userRepository.findByImageIdIsNotNull()).thenReturn(List.of(user("anna", "old.png")));
        when(handleAvatarService.needsProcessing("old.png")).thenReturn(true);
        when(handleAvatarService.reprocessStoredAvatar("old.png")).thenReturn("new.jpg");
        doThrow(new IllegalStateException("database down")).when(userRepository).updateImageId("anna", "new.jpg");

        runner.run(null);

        verify(handleAvatarService, never()).deleteAvatar("old.png");
    }

    @Test
    void groupAvatars_AreConvertedToo() throws IOException {
        Group group = new Group();
        group.setId(7L);
        group.setImageId("group.png");
        when(userRepository.findByImageIdIsNotNull()).thenReturn(List.of());
        when(groupRepository.findAll()).thenReturn(List.of(group));
        when(handleAvatarService.needsProcessing("group.png")).thenReturn(true);
        when(handleAvatarService.reprocessStoredAvatar("group.png")).thenReturn("new.jpg");

        runner.run(null);

        verify(groupRepository).updateImageId(7L, "new.jpg");
        verify(handleAvatarService).deleteAvatar("group.png");
        verify(userRepository, never()).updateImageId(anyString(), any());
    }
}

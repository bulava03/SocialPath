package com.socialpath;

import com.socialpath.dto.request.UserUpdate;
import com.socialpath.entity.User;
import com.socialpath.exception.InvalidImageException;
import com.socialpath.media.AvatarImageProcessor;
import com.socialpath.media.AvatarImageProcessor.ProcessedAvatar;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.impl.HandleAvatarServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandleAvatarServiceTest {

    private static final String OLD_ID = "11111111-1111-1111-1111-111111111111.jpg";
    private static final String OLD_THUMB_ID = "11111111-1111-1111-1111-111111111111_thumb.jpg";

    private FileStorageService fileStorageService;
    private AvatarImageProcessor avatarImageProcessor;
    private HandleAvatarServiceImpl service;
    private User myUser;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        avatarImageProcessor = mock(AvatarImageProcessor.class);
        service = new HandleAvatarServiceImpl(fileStorageService, avatarImageProcessor);
        myUser = new User();
        myUser.setImageId(OLD_ID);
    }

    @Test
    void updateAvatar_RejectedUpload_KeepsCurrentAvatar() throws IOException {
        // Arrange
        when(avatarImageProcessor.process(any(InputStreamSource.class)))
                .thenThrow(new InvalidImageException("Unsupported image format."));
        MockMultipartFile upload = new MockMultipartFile("file", "a.txt", "text/plain", new byte[]{1, 2, 3});

        // Act & Assert
        assertThrows(InvalidImageException.class, () -> service.updateAvatar(new UserUpdate(), myUser, upload));
        verify(fileStorageService, never()).deleteFileById(anyString());
        verify(fileStorageService, never()).storeFile(anyString(), any(byte[].class));
    }

    @Test
    void updateAvatar_ValidUpload_StoresBothVersionsThenDeletesOldOnes() throws IOException {
        // Arrange
        byte[] full = {1};
        byte[] thumbnail = {2};
        when(avatarImageProcessor.process(any(InputStreamSource.class))).thenReturn(new ProcessedAvatar(full, thumbnail));
        MockMultipartFile upload = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});

        // Act
        UserUpdate result = service.updateAvatar(new UserUpdate(), myUser, upload);

        // Assert
        String newId = result.getImageId();
        assertTrue(newId.endsWith(".jpg"));
        verify(fileStorageService).storeFile(newId, full);
        verify(fileStorageService).storeFile(eq(newId.replace(".jpg", "_thumb.jpg")), eq(thumbnail));
        verify(fileStorageService).deleteFileById(OLD_ID);
        verify(fileStorageService).deleteFileById(OLD_THUMB_ID);
    }

    @Test
    void updateAvatar_RemoveFlag_DeletesBothVersions() throws IOException {
        // Arrange
        UserUpdate update = new UserUpdate();
        update.setRemoveImage(true);

        // Act
        UserUpdate result = service.updateAvatar(update, myUser, null);

        // Assert
        assertEquals("", result.getImageId());
        verify(fileStorageService).deleteFileById(OLD_ID);
        verify(fileStorageService).deleteFileById(OLD_THUMB_ID);
    }
}

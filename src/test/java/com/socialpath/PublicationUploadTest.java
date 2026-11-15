package com.socialpath;

import com.socialpath.config.FeedProperties;
import com.socialpath.dto.request.NewComment;
import com.socialpath.entity.Publication;
import com.socialpath.media.PublicationMediaRules;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.GroupService;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Media of a new post or comment is checked on the server before anything is stored. */
class PublicationUploadTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};

    private CommentsRepository commentsRepository;
    private FileStorageService fileStorageService;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        fileStorageService = mock(FileStorageService.class);
        service = new CommentsServiceImpl(mock(UserRepository.class), commentsRepository, fileStorageService,
                mock(VideoService.class), new FeedProperties(20, 10, 1), mock(GroupService.class));
        // The post the test comments answer.
        Publication post = new Publication();
        post.setId(1L);
        when(commentsRepository.findById(1L)).thenReturn(Optional.of(post));
    }

    private static NewComment comment(MultipartFile... media) {
        NewComment comment = new NewComment();
        comment.setIdPublication(1L);
        comment.setText("hi");
        comment.setMedia(List.of(media));
        return comment;
    }

    @Test
    void forgedUpload_IsRefused_AndNothingIsStored() throws Exception {
        MultipartFile png = new MockMultipartFile("media", "fine.png", "image/png", PNG);
        MultipartFile html = new MockMultipartFile("media", "photo.jpg", "image/jpeg",
                "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.commentOnUserPage("anna", comment(png, html)));

        assertEquals(PublicationMediaRules.NOT_ALLOWED, error.getMessage());
        // Not even the valid first file was written.
        verify(fileStorageService, never()).storeFile(any(MultipartFile.class), anyString());
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void storedFile_GetsTheExtensionOfItsContent_NotOfItsName() throws Exception {
        MultipartFile png = new MockMultipartFile("media", "picture.html", "text/html", PNG);
        when(fileStorageService.storeFile(any(MultipartFile.class), anyString())).thenReturn("id.png");
        when(commentsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.commentOnUserPage("anna", comment(png));

        verify(fileStorageService).storeFile(eq(png), eq("png"));
    }
}

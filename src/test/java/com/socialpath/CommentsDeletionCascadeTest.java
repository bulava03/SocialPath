package com.socialpath;

import com.socialpath.config.FeedProperties;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The application's part of deleting a thread: the rows are removed by the
 * stored procedure delete_thread, which returns the media file ids of the
 * whole tree; the service removes those files, which the database cannot
 * do. Without an active transaction (as in these unit tests) the files are
 * removed immediately; inside a transaction the cleanup runs after commit.
 * The procedure itself, a thread deeper than the database's limits
 * included, is tested against MySQL in CommentsRepositoryTest.
 */
class CommentsDeletionCascadeTest {

    private CommentsRepository commentsRepository;
    private FileStorageService fileStorageService;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        fileStorageService = mock(FileStorageService.class);
        service = new CommentsServiceImpl(
                mock(UserRepository.class),
                commentsRepository,
                fileStorageService,
                mock(VideoService.class), new FeedProperties(20, 10, 1));
    }

    @Test
    void deletePublication_RemovesTheFilesOfTheWholeDeletedTree() {
        when(commentsRepository.deleteThread(1L)).thenReturn(List.of("photo1", "photo2", "photo3"));

        service.deletePublication(1L);

        verify(commentsRepository).deleteThread(1L);
        verify(fileStorageService).deleteFileById("photo1");
        verify(fileStorageService).deleteFileById("photo2");
        verify(fileStorageService).deleteFileById("photo3");
    }

    @Test
    void deletePublication_WithoutMedia_RemovesNoFiles() {
        when(commentsRepository.deleteThread(1L)).thenReturn(List.of());

        service.deletePublication(1L);

        verify(fileStorageService, never()).deleteFileById(anyString());
    }
}

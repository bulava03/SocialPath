package com.socialpath;

import com.socialpath.dto.request.DelComment;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the delete invariant of the publication tree. Rows are removed by
 * the database's ON DELETE CASCADE from a single root delete; the service is
 * responsible for the part the database cannot do — collecting the media
 * file ids of the whole subtree (recursive CTE) and removing those files.
 * Without an active transaction (as in these unit tests) the files are
 * removed immediately; inside a transaction the cleanup runs after commit.
 */
class CommentsDeletionCascadeTest {

    private CommentsRepository commentsRepository;
    private FileStorageService fileStorageService;
    private CommentsServiceImpl service;

    private final Long publicationId = 1L;
    private final Long commentId = 2L;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        fileStorageService = mock(FileStorageService.class);
        service = new CommentsServiceImpl(
                mock(UserRepository.class),
                mock(GroupRepository.class),
                commentsRepository,
                fileStorageService);
    }

    @Test
    void removePublicationUser_DeletesRootRowAndMediaOfWholeSubtree() {
        // publication (photo1) -> comment (photo2) -> reply (photo3)
        when(commentsRepository.findSubtreeMediaFileIds(publicationId))
                .thenReturn(List.of("photo1", "photo2", "photo3"));

        DelComment delComment = new DelComment();
        delComment.setIdPublication("publications");
        delComment.setIdComment(publicationId);
        delComment.setLogin("alice");

        service.removePublicationUser(delComment);

        // One root delete; the database cascade removes the subtree rows.
        verify(commentsRepository).deleteById(publicationId);
        // Media files of every node in the subtree are cleaned up.
        verify(fileStorageService).deleteFileById("photo1");
        verify(fileStorageService).deleteFileById("photo2");
        verify(fileStorageService).deleteFileById("photo3");
    }

    @Test
    void removeComment_DeletesOnlyItsOwnSubtree() {
        when(commentsRepository.findSubtreeMediaFileIds(commentId))
                .thenReturn(List.of("photo2"));

        DelComment delComment = new DelComment();
        delComment.setIdPublication(publicationId.toString());
        delComment.setIdComment(commentId);

        service.removeComment(delComment);

        verify(commentsRepository).deleteById(commentId);
        verify(commentsRepository, never()).deleteById(publicationId);
        verify(fileStorageService).deleteFileById("photo2");
    }
}

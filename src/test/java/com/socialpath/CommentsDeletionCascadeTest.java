package com.socialpath;

import com.socialpath.document.Publication;
import com.socialpath.dto.request.DelComment;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the delete cascade invariant: removing a publication must delete the
 * media files of every node in its comment subtree, not only the root's, and
 * must delete every document of the subtree.
 */
class CommentsDeletionCascadeTest {

    private CommentsRepository commentsRepository;
    private FileStorageService fileStorageService;
    private UserRepository userRepository;
    private CommentsServiceImpl service;

    private final ObjectId publicationId = new ObjectId();
    private final ObjectId commentId = new ObjectId();
    private final ObjectId replyId = new ObjectId();

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        fileStorageService = mock(FileStorageService.class);
        userRepository = mock(UserRepository.class);
        service = new CommentsServiceImpl(
                userRepository,
                mock(GroupRepository.class),
                commentsRepository,
                mock(ModelMapper.class),
                fileStorageService);
    }

    private Publication node(ObjectId id, List<String> media, List<ObjectId> comments) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setAuthorId("alice");
        publication.setMedia(media);
        publication.setComments(comments);
        return publication;
    }

    @Test
    void removePublicationUser_DeletesMediaOfWholeSubtree() {
        // publication (photo1) -> comment (photo2) -> reply (photo3)
        when(commentsRepository.findById(publicationId))
                .thenReturn(Optional.of(node(publicationId, List.of("photo1"), List.of(commentId))));
        when(commentsRepository.findById(commentId))
                .thenReturn(Optional.of(node(commentId, List.of("photo2"), List.of(replyId))));
        when(commentsRepository.findById(replyId))
                .thenReturn(Optional.of(node(replyId, List.of("photo3"), null)));

        DelComment delComment = new DelComment();
        delComment.setIdPublication("publications");
        delComment.setIdComment(publicationId);
        delComment.setLogin("alice");

        service.removePublicationUser(delComment);

        verify(fileStorageService).deleteFileById("photo1");
        verify(fileStorageService).deleteFileById("photo2");
        verify(fileStorageService).deleteFileById("photo3");
        verify(userRepository).removePublicationFromUser("alice", publicationId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObjectId>> deleted = ArgumentCaptor.forClass(List.class);
        verify(commentsRepository).deleteAllById(deleted.capture());
        assertEquals(3, deleted.getValue().size());
        assertTrue(deleted.getValue().containsAll(List.of(publicationId, commentId, replyId)));
    }

    @Test
    void removeComment_DeletesOnlyItsOwnSubtree() {
        when(commentsRepository.findById(commentId))
                .thenReturn(Optional.of(node(commentId, List.of("photo2"), null)));

        DelComment delComment = new DelComment();
        delComment.setIdPublication(publicationId.toString());
        delComment.setIdComment(commentId);

        service.removeComment(delComment);

        verify(fileStorageService).deleteFileById("photo2");
        verify(commentsRepository).removeCommentFromPublication(publicationId, commentId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObjectId>> deleted = ArgumentCaptor.forClass(List.class);
        verify(commentsRepository).deleteAllById(deleted.capture());
        assertEquals(List.of(commentId), deleted.getValue());
    }
}

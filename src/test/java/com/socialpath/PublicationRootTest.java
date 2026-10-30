package com.socialpath;

import com.socialpath.entity.Publication;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import com.socialpath.service.impl.CommentsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Permission checks rely on findRoot to learn where a comment thread lives. */
class PublicationRootTest {

    private CommentsRepository commentsRepository;
    private CommentsServiceImpl service;

    @BeforeEach
    void setUp() {
        commentsRepository = mock(CommentsRepository.class);
        service = new CommentsServiceImpl(
                mock(UserRepository.class),
                commentsRepository,
                mock(FileStorageService.class),
                mock(VideoService.class));
    }

    private Publication publication(Long id, Long parentId) {
        Publication publication = new Publication();
        publication.setId(id);
        publication.setParentId(parentId);
        when(commentsRepository.findById(id)).thenReturn(Optional.of(publication));
        return publication;
    }

    @Test
    void findRoot_OfTopLevelPublication_IsItself() {
        Publication root = publication(1L, null);

        assertSame(root, service.findRoot(root));
    }

    @Test
    void findRoot_OfNestedReply_IsTheTopLevelPublication() {
        Publication root = publication(1L, null);
        publication(2L, 1L);
        Publication reply = publication(3L, 2L);

        assertSame(root, service.findRoot(reply));
    }
}

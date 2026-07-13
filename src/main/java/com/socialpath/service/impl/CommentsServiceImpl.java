package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.Group;
import com.socialpath.document.Publication;
import com.socialpath.document.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CommentsService;
import com.socialpath.service.FileStorageService;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final CommentsRepository commentsRepository;
    private final ModelMapper modelMapper;
    private final FileStorageService fileStorageService;

    @Override
    public void addNewUserPublication(NewPublication newPublication) throws IOException {
        Publication publication = modelMapper.map(newPublication, Publication.class);

        if (newPublication.getMedia() == null) {
            newPublication.setMedia(new ArrayList<>());
        }

        List<String> mediaIds = new ArrayList<>();
        for (MultipartFile file : newPublication.getMedia()) {
            mediaIds.add(fileStorageService.storeFile(file));
        }
        publication.setMedia(mediaIds);

        publication.setCreatedAt(LocalDateTime.now());
        Publication savedPublication = commentsRepository.save(publication);
        ObjectId publicationId = savedPublication.getId();
        userRepository.addPublicationToUser(publicationId, newPublication.getAuthorId());
    }

    @Override
    public void addNewGroupPublication(NewPublication newPublication) throws IOException {
        Publication publication = modelMapper.map(newPublication, Publication.class);

        if (newPublication.getMedia() == null) {
            newPublication.setMedia(new ArrayList<>());
        }

        List<String> mediaIds = new ArrayList<>();
        for (MultipartFile file : newPublication.getMedia()) {
            mediaIds.add(fileStorageService.storeFile(file));
        }
        publication.setMedia(mediaIds);

        publication.setCreatedAt(LocalDateTime.now());
        Publication savedPublication = commentsRepository.save(publication);
        ObjectId publicationId = savedPublication.getId();
        groupRepository.addPublicationToGroup(publicationId, new ObjectId(newPublication.getGroupId()));
    }

    @Override
    public List<PublicationPresentable> loadComments(String type, String idInType) throws IOException {
        List<ObjectId> ids;
        if (type.equals("User")) {
            ids = userRepository.getPublicationsIdList(idInType);
        } else {
            Group group = groupRepository.findById(new ObjectId(idInType)).orElse(null);
            if (group != null) {
                return getPublications(group.getPublications());
            } else {
                return null;
            }
        }
        return getPublications(ids);
    }

    @Override
    public List<ObjectId> getCommentsIdsUser(String login) {
        User user = userRepository.findByLogin(login);
        if (user != null) {
            if (user.getPublications() != null) {
                return user.getPublications();
            } else {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<ObjectId> getCommentsIdsGroup(ObjectId groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group != null) {
            if (group.getPublications() != null) {
                return group.getPublications();
            } else {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    @Override
    public List<PublicationPresentable> loadComments(List<ObjectId> commentIds) throws IOException {
        return getPublications(commentIds);
    }

    @Override
    public Publication findById(ObjectId id) {
        return commentsRepository.findById(id).orElse(null);
    }

    @Override
    public List<PublicationPresentable> getPublications(List<ObjectId> ids) throws IOException {
        if (ids == null) {
            return null;
        }

        List<PublicationPresentable> publications = new ArrayList<>();
        for (ObjectId element : ids) {
            Publication publication = commentsRepository.findById(element).orElse(null);
            if (publication == null) {
                continue;
            }
            publications.add(toPresentable(publication));
        }
        return publications;
    }

    /**
     * Builds the view model for one publication, including its author info,
     * media ids (served via /images/{id}) and nested comments.
     * @param publication the stored publication
     * @return the presentable form of the publication
     */
    private PublicationPresentable toPresentable(Publication publication) throws IOException {
        User author = userRepository.findByLogin(publication.getAuthorId());

        List<PublicationPresentable> publicationComments = publication.getComments() != null
                ? getPublications(publication.getComments())
                : null;

        List<String> media = publication.getMedia() != null
                ? publication.getMedia()
                : new ArrayList<>();

        return new PublicationPresentable(
                publication.getId(),
                author != null ? author.getLogin() : publication.getAuthorId(),
                resolveFullName(author, publication.getAuthorId()),
                resolveAuthorImageId(author),
                publication.getText(),
                publication.getCreatedAt(),
                media,
                containsVideo(media),
                publicationComments
        );
    }

    private String resolveAuthorImageId(User author) {
        if (author == null || author.getImageId() == null || author.getImageId().isEmpty()) {
            return null;
        }
        return author.getImageId();
    }

    private String resolveFullName(User author, String fallbackLogin) {
        if (author == null) {
            return fallbackLogin;
        }
        if (author.getFirstName() != null && author.getLastName() != null) {
            return author.getFirstName() + " " + author.getLastName();
        }
        return author.getLogin();
    }

    private boolean containsVideo(List<String> media) {
        for (String elem : media) {
            if (fileStorageService.isVideo(elem)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addNewComment(NewComment newComment) throws IOException {
        Publication comment = modelMapper.map(newComment, Publication.class);

        if (newComment.getMedia() == null) {
            newComment.setMedia(new ArrayList<>());
        }

        List<String> mediaIds = new ArrayList<>();
        for (MultipartFile file : newComment.getMedia()) {
            mediaIds.add(fileStorageService.storeFile(file));
        }
        comment.setMedia(mediaIds);

        comment.setCreatedAt(LocalDateTime.now());
        comment.setAuthorId(newComment.getAuthorLogin());
        Publication savedComment = commentsRepository.save(comment);
        ObjectId commentId = savedComment.getId();
        commentsRepository.pushCommentId(commentId, newComment.getIdPublication());
    }

    @Override
    public void removePublicationUser(DelComment delComment) {
        userRepository.removePublicationFromUser(delComment.getLogin(), delComment.getIdComment());
        deletePublicationTree(delComment.getIdComment());
    }

    @Override
    public void removePublicationGroup(DelComment delComment) {
        groupRepository.removePublicationFromGroup(new ObjectId(delComment.getGroupId()), delComment.getIdComment());
        deletePublicationTree(delComment.getIdComment());
    }

    @Override
    public void removeComment(DelComment delComment) {
        commentsRepository.removeCommentFromPublication(new ObjectId(delComment.getIdPublication()), delComment.getIdComment());
        deletePublicationTree(delComment.getIdComment());
    }

    /**
     * Deletes a publication together with its whole comment subtree: first the
     * media files of every node in the tree, then all documents in one batch.
     * This is the single owner of the delete cascade; keeping the file cleanup
     * and the document cleanup side by side is what guarantees neither
     * documents nor GridFS files are ever orphaned.
     * @param rootId id of the publication or comment to delete
     */
    private void deletePublicationTree(ObjectId rootId) {
        List<Publication> tree = new ArrayList<>();
        collectTree(rootId, tree);

        for (Publication node : tree) {
            if (node.getMedia() != null) {
                for (String fileId : node.getMedia()) {
                    fileStorageService.deleteFileById(fileId);
                }
            }
        }

        commentsRepository.deleteAllById(tree.stream().map(Publication::getId).toList());
    }

    private void collectTree(ObjectId id, List<Publication> accumulator) {
        Publication publication = commentsRepository.findById(id).orElse(null);
        if (publication == null) {
            return;
        }
        accumulator.add(publication);
        if (publication.getComments() != null) {
            for (ObjectId childId : publication.getComments()) {
                collectTree(childId, accumulator);
            }
        }
    }

}

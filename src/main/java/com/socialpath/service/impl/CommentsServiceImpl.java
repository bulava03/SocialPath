package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Group;
import com.socialpath.entity.Publication;
import com.socialpath.entity.User;
import com.socialpath.dto.request.DelComment;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CommentsService;
import com.socialpath.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Publications and comments over one self-referencing table. Where the
 * document model attached publications to a user/group array and comments to
 * a parent's comments array, here placement is expressed by the row itself:
 * author_login for user pages, group_id for group pages, parent_id for
 * comments. Creating and deleting therefore touch a single aggregate.
 */
@Service
@RequiredArgsConstructor
public class CommentsServiceImpl implements CommentsService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final CommentsRepository commentsRepository;
    private final FileStorageService fileStorageService;

    @Override
    public void addNewUserPublication(NewPublication newPublication) throws IOException {
        Publication publication = newPublication(newPublication.getText(), newPublication.getAuthorId(),
                newPublication.getMedia());
        commentsRepository.save(publication);
    }

    @Override
    public void addNewGroupPublication(NewPublication newPublication) throws IOException {
        Publication publication = newPublication(newPublication.getText(), newPublication.getAuthorId(),
                newPublication.getMedia());
        publication.setGroupId(Long.valueOf(newPublication.getGroupId()));
        commentsRepository.save(publication);
    }

    @Override
    public List<PublicationPresentable> loadComments(String type, String idInType) throws IOException {
        if (type.equals("User")) {
            return getPublications(commentsRepository.findUserPublicationIds(idInType));
        }
        Group group = groupRepository.findById(Long.valueOf(idInType)).orElse(null);
        if (group == null) {
            return null;
        }
        return getPublications(commentsRepository.findGroupPublicationIds(group.getId()));
    }

    @Override
    public List<Long> getCommentsIdsUser(String login) {
        User user = userRepository.findByLogin(login);
        if (user == null) {
            return new ArrayList<>();
        }
        return commentsRepository.findUserPublicationIds(login);
    }

    @Override
    public List<Long> getCommentsIdsGroup(Long groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return new ArrayList<>();
        }
        return commentsRepository.findGroupPublicationIds(groupId);
    }

    @Override
    public List<PublicationPresentable> loadComments(List<Long> commentIds) throws IOException {
        return getPublications(commentIds);
    }

    @Override
    public Publication findById(Long id) {
        return commentsRepository.findById(id).orElse(null);
    }

    @Override
    public List<PublicationPresentable> getPublications(List<Long> ids) throws IOException {
        if (ids == null) {
            return null;
        }

        List<PublicationPresentable> publications = new ArrayList<>();
        for (Long element : ids) {
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
     * media ids (served via /images/{id}) and nested comments, loaded by
     * parent_id in insertion order.
     * @param publication the stored publication
     * @return the presentable form of the publication
     */
    private PublicationPresentable toPresentable(Publication publication) throws IOException {
        User author = userRepository.findByLogin(publication.getAuthorId());

        List<Publication> children = commentsRepository.findByParentIdOrderByCreatedAtAscIdAsc(publication.getId());
        List<PublicationPresentable> publicationComments = new ArrayList<>();
        for (Publication child : children) {
            publicationComments.add(toPresentable(child));
        }

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
        Publication comment = newPublication(newComment.getText(), newComment.getAuthorLogin(),
                newComment.getMedia());
        comment.setParentId(newComment.getIdPublication());
        commentsRepository.save(comment);
    }

    /**
     * Builds a fresh publication entity. Explicit construction instead of
     * ModelMapper on purpose: the request DTOs carry groupId/authorId
     * properties whose names partially match the entity's Long id, which
     * makes implicit mapping ambiguous (and it also has nothing sensible to
     * do with the multipart media list).
     * @param text publication text
     * @param authorLogin the author's login
     * @param files uploaded media, may be null
     * @return a new, unsaved publication with media stored and timestamp set
     */
    private Publication newPublication(String text, String authorLogin,
                                       List<MultipartFile> files) throws IOException {
        Publication publication = new Publication();
        publication.setText(text);
        publication.setAuthorId(authorLogin);
        publication.setMedia(storeMedia(files));
        publication.setCreatedAt(LocalDateTime.now());
        return publication;
    }

    @Override
    @Transactional
    public void removePublicationUser(DelComment delComment) {
        deletePublicationTree(delComment.getIdComment());
    }

    @Override
    @Transactional
    public void removePublicationGroup(DelComment delComment) {
        deletePublicationTree(delComment.getIdComment());
    }

    @Override
    @Transactional
    public void removeComment(DelComment delComment) {
        deletePublicationTree(delComment.getIdComment());
    }

    private List<String> storeMedia(List<MultipartFile> files) throws IOException {
        List<String> mediaIds = new ArrayList<>();
        if (files == null) {
            return mediaIds;
        }
        for (MultipartFile file : files) {
            mediaIds.add(fileStorageService.storeFile(file));
        }
        return mediaIds;
    }

    /**
     * Deletes a publication together with its whole comment subtree. The
     * database owns the row cascade: deleting the root row lets ON DELETE
     * CASCADE atomically remove the subtree and its publication_media rows.
     * The application's job is the part the database cannot do — the media
     * files on disk. Their ids are collected up front for the whole subtree
     * with one recursive CTE, and the files are removed only after the
     * transaction commits: a rollback must never leave rows pointing at
     * already-deleted files.
     * @param rootId id of the publication or comment to delete
     */
    private void deletePublicationTree(Long rootId) {
        List<String> fileIds = commentsRepository.findSubtreeMediaFileIds(rootId);
        commentsRepository.deleteById(rootId);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileIds.forEach(fileStorageService::deleteFileById);
                }
            });
        } else {
            fileIds.forEach(fileStorageService::deleteFileById);
        }
    }
}

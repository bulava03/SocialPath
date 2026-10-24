package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.Publication;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.repository.AuthorView;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CommentsService;
import com.socialpath.service.FileStorageService;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final CommentsRepository commentsRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Long addNewUserPublication(NewPublication newPublication) throws IOException {
        Publication publication = newPublication(newPublication.getText(), newPublication.getAuthorId(),
                newPublication.getMedia());
        return commentsRepository.save(publication).getId();
    }

    @Override
    public Long addNewGroupPublication(NewPublication newPublication) throws IOException {
        Publication publication = newPublication(newPublication.getText(), newPublication.getAuthorId(),
                newPublication.getMedia());
        publication.setGroupId(Long.valueOf(newPublication.getGroupId()));
        return commentsRepository.save(publication).getId();
    }

    @Override
    public FeedPage loadUserFeed(String login, Long before) {
        return toPage(commentsRepository.findUserFeed(login, before, Limit.of(FEED_PAGE_SIZE + 1)));
    }

    @Override
    public FeedPage loadGroupFeed(Long groupId, Long before) {
        return toPage(commentsRepository.findGroupFeed(groupId, before, Limit.of(FEED_PAGE_SIZE + 1)));
    }

    @Override
    public PublicationPresentable loadThread(Publication post) {
        return buildFeed(List.of(findRoot(post))).get(0);
    }

    @Override
    public long latestUserFeedActivity(String owner, String viewer) {
        Long latest = commentsRepository.findLatestUserFeedActivity(owner, viewer);
        return latest == null ? 0 : latest;
    }

    @Override
    public long latestGroupFeedActivity(Long groupId, String viewer) {
        Long latest = commentsRepository.findLatestGroupFeedActivity(groupId, viewer);
        return latest == null ? 0 : latest;
    }

    /**
     * Cuts a portion from posts queried with one extra row: that row only
     * tells whether anything older exists and is not shown.
     * @param rows up to FEED_PAGE_SIZE + 1 posts, newest first
     * @return the portion and where to continue
     */
    private FeedPage toPage(List<Publication> rows) {
        boolean hasMore = rows.size() > FEED_PAGE_SIZE;
        List<Publication> posts = hasMore ? rows.subList(0, FEED_PAGE_SIZE) : rows;
        Long nextBefore = hasMore ? posts.get(posts.size() - 1).getId() : null;
        return new FeedPage(buildFeed(posts), nextBefore);
    }

    @Override
    public Publication findById(Long id) {
        return commentsRepository.findById(id).orElse(null);
    }

    @Override
    public Publication findRoot(Publication publication) {
        Publication current = publication;
        while (current.getParentId() != null) {
            Publication parent = findById(current.getParentId());
            if (parent == null) {
                break;
            }
            current = parent;
        }
        return current;
    }

    /**
     * Turns the posts of a page into view models with their whole comment
     * trees. The size of the page does not change the number of queries:
     * one for all comments, one for all authors (media come batched with
     * the publications), and the tree is assembled in memory.
     * @param posts the portion's top-level publications, newest first
     * @return the posts as view models
     */
    private List<PublicationPresentable> buildFeed(List<Publication> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Publication> replies = commentsRepository.findReplies(posts.stream().map(Publication::getId).toList());

        Map<Long, List<Publication>> repliesByParent = replies.stream()
                .collect(Collectors.groupingBy(Publication::getParentId));
        Set<String> authorLogins = Stream.concat(posts.stream(), replies.stream())
                .map(Publication::getAuthorId)
                .collect(Collectors.toSet());
        Map<String, AuthorView> authors = userRepository.findAuthors(authorLogins).stream()
                .collect(Collectors.toMap(AuthorView::getLogin, Function.identity()));

        return posts.stream()
                .map(post -> toPresentable(post, repliesByParent, authors))
                .toList();
    }

    /**
     * Builds the view model for one publication and, recursively, its replies.
     * @param publication the stored publication
     * @param repliesByParent all replies of the feed, grouped by parent id
     * @param authors the feed's authors by login
     * @return the presentable form of the publication
     */
    private PublicationPresentable toPresentable(Publication publication,
                                                 Map<Long, List<Publication>> repliesByParent,
                                                 Map<String, AuthorView> authors) {
        List<PublicationPresentable> replies = repliesByParent
                .getOrDefault(publication.getId(), List.of()).stream()
                .map(reply -> toPresentable(reply, repliesByParent, authors))
                .toList();
        List<String> media = publication.getMedia() != null ? publication.getMedia() : List.of();
        AuthorView author = authors.get(publication.getAuthorId());

        return new PublicationPresentable(
                publication.getId(),
                publication.getAuthorId(),
                fullName(author, publication.getAuthorId()),
                author != null && author.getImageId() != null && !author.getImageId().isEmpty()
                        ? author.getImageId() : null,
                publication.getText(),
                publication.getCreatedAt(),
                media,
                containsVideo(media),
                replies
        );
    }

    private String fullName(AuthorView author, String login) {
        if (author == null) {
            return login;
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
    @Override
    @Transactional
    public void deletePublication(Long rootId) {
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

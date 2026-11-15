package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.config.FeedProperties;
import com.socialpath.entity.Publication;
import com.socialpath.dto.request.NewComment;
import com.socialpath.dto.request.NewPublication;
import com.socialpath.dto.response.FeedPage;
import com.socialpath.dto.response.PublicationPresentable;
import com.socialpath.dto.response.RepliesPage;
import com.socialpath.entity.VideoJob;
import com.socialpath.media.PublicationMediaRules;
import com.socialpath.media.PublicationMediaType;
import com.socialpath.repository.AuthorView;
import com.socialpath.repository.CommentsRepository;
import com.socialpath.repository.ReplyCount;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CommentsService;
import com.socialpath.service.FileStorageService;
import com.socialpath.service.VideoService;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final VideoService videoService;
    private final FeedProperties feed;

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
        return toPage(commentsRepository.findUserFeed(login, before, Limit.of(feed.pageSize() + 1)));
    }

    @Override
    public FeedPage loadGroupFeed(Long groupId, Long before) {
        return toPage(commentsRepository.findGroupFeed(groupId, before, Limit.of(feed.pageSize() + 1)));
    }

    @Override
    public RepliesPage loadReplies(Long parentId, Long before) {
        List<Publication> rows = commentsRepository.findReplies(parentId, before, Limit.of(feed.repliesPageSize()));
        if (rows.isEmpty()) {
            return new RepliesPage(List.of(), null, 0, 0);
        }
        Long oldest = rows.get(rows.size() - 1).getId();
        long hidden = commentsRepository.countByParentIdAndIdLessThan(parentId, oldest);
        return new RepliesPage(present(rows, 0), hidden > 0 ? oldest : null, hidden,
                Math.min(hidden, feed.repliesPageSize()));
    }

    @Override
    public PublicationPresentable loadPublication(Publication publication) {
        return present(List.of(publication), 0).get(0);
    }

    @Override
    public long latestUserFeedActivity(String owner, String viewer) {
        Long latest = commentsRepository.findLatestUserPost(owner, viewer);
        return latest == null ? 0 : latest;
    }

    @Override
    public long latestGroupFeedActivity(Long groupId, String viewer) {
        Long latest = commentsRepository.findLatestGroupPost(groupId, viewer);
        return latest == null ? 0 : latest;
    }

    /**
     * Cuts a portion from posts queried with one extra row: that row only
     * tells whether anything older exists and is not shown.
     * @param rows up to page-size + 1 posts, newest first
     * @return the portion and where to continue
     */
    private FeedPage toPage(List<Publication> rows) {
        boolean hasMore = rows.size() > feed.pageSize();
        List<Publication> posts = hasMore ? rows.subList(0, feed.pageSize()) : rows;
        Long nextBefore = hasMore ? posts.get(posts.size() - 1).getId() : null;
        return new FeedPage(present(posts, feed.repliesPreloadDepth()), nextBefore);
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
     * Turns posts or comments into view models with the first portions of
     * their replies, a given number of levels deep. Every level is bounded:
     * at most repliesPageSize replies under each parent, the rest only
     * counted. The number of queries depends on the depth only, not on how
     * many publications there are: one per level of replies, one counting
     * the replies of everything loaded, one for the authors (media come
     * batched with the publications) and one for the conversion status of
     * any videos; the tree is assembled in memory.
     * @param nodes the publications to present, in display order
     * @param depth how many levels of replies to load under them
     * @return the publications as view models
     */
    private List<PublicationPresentable> present(List<Publication> nodes, int depth) {
        if (nodes.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Publication>> repliesByParent = new HashMap<>();
        List<Publication> loaded = new ArrayList<>(nodes);
        List<Publication> level = nodes;
        for (int i = 0; i < depth && !level.isEmpty(); i++) {
            level = commentsRepository.findNewestReplies(idsOf(level), feed.repliesPageSize());
            for (Publication reply : level) {
                repliesByParent.computeIfAbsent(reply.getParentId(), parent -> new ArrayList<>()).add(reply);
            }
            loaded.addAll(level);
        }

        Map<Long, Long> replyCounts = commentsRepository.countReplies(idsOf(loaded)).stream()
                .collect(Collectors.toMap(ReplyCount::getParentId, ReplyCount::getTotal));
        Set<String> authorLogins = loaded.stream()
                .map(Publication::getAuthorId)
                .collect(Collectors.toSet());
        Map<String, AuthorView> authors = userRepository.findAuthors(authorLogins).stream()
                .collect(Collectors.toMap(AuthorView::getLogin, Function.identity()));
        Set<String> videoIds = loaded.stream()
                .flatMap(publication -> mediaOf(publication).stream())
                .filter(CommentsServiceImpl::isVideo)
                .collect(Collectors.toSet());
        Map<String, VideoJob.Status> videoStatuses = videoService.statusOf(videoIds);

        FeedParts parts = new FeedParts(repliesByParent, replyCounts, authors, videoStatuses);
        return nodes.stream()
                .map(node -> toPresentable(node, parts))
                .toList();
    }

    private static List<Long> idsOf(List<Publication> publications) {
        return publications.stream().map(Publication::getId).toList();
    }

    /**
     * What view models are assembled from.
     * @param repliesByParent the loaded replies, grouped by parent id, newest first
     * @param replyCounts how many direct replies each loaded publication has
     * @param authors the authors by login
     * @param videoStatuses conversion status of queued videos
     */
    private record FeedParts(Map<Long, List<Publication>> repliesByParent,
                             Map<Long, Long> replyCounts,
                             Map<String, AuthorView> authors,
                             Map<String, VideoJob.Status> videoStatuses) {
    }

    /**
     * Builds the view model for one publication and, recursively, its loaded
     * replies. Replies that are not loaded are left behind a button: counted,
     * with the id they continue below when part of them is shown.
     * @param publication the stored publication
     * @param parts the loaded replies, reply counts, authors and video statuses
     * @return the presentable form of the publication
     */
    private PublicationPresentable toPresentable(Publication publication, FeedParts parts) {
        List<Publication> loadedReplies = parts.repliesByParent().getOrDefault(publication.getId(), List.of());
        List<PublicationPresentable> replies = loadedReplies.stream()
                .map(reply -> toPresentable(reply, parts))
                .toList();
        long hiddenReplies = Math.max(0, parts.replyCounts().getOrDefault(publication.getId(), 0L)
                - loadedReplies.size());
        Long repliesBefore = hiddenReplies > 0 && !loadedReplies.isEmpty()
                ? loadedReplies.get(loadedReplies.size() - 1).getId() : null;
        List<String> media = mediaOf(publication);
        AuthorView author = parts.authors().get(publication.getAuthorId());
        boolean video = media.stream().anyMatch(CommentsServiceImpl::isVideo);

        return new PublicationPresentable(
                publication.getId(),
                publication.getAuthorId(),
                fullName(author, publication.getAuthorId()),
                author != null && author.getImageId() != null && !author.getImageId().isEmpty()
                        ? author.getImageId() : null,
                publication.getText(),
                publication.getCreatedAt(),
                media,
                video,
                video ? VideoService.displayStatus(parts.videoStatuses().get(media.get(0))) : null,
                replies,
                hiddenReplies,
                Math.min(hiddenReplies, feed.repliesPageSize()),
                repliesBefore
        );
    }

    private static List<String> mediaOf(Publication publication) {
        return publication.getMedia() != null ? publication.getMedia() : List.of();
    }

    /**
     * Videos are told by the extension the server gave them; no file system access needed.
     * @param fileId a stored media file id
     * @return true for a video
     */
    private static boolean isVideo(String fileId) {
        return PublicationMediaType.ofFileId(fileId).map(PublicationMediaType::isVideo).orElse(false);
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

    @Override
    public Long addNewComment(NewComment newComment) throws IOException {
        Publication comment = newPublication(newComment.getText(), newComment.getAuthorLogin(),
                newComment.getMedia());
        comment.setParentId(newComment.getIdPublication());
        return commentsRepository.save(comment).getId();
    }

    /**
     * Builds a fresh publication entity. Constructed explicitly on purpose:
     * the request DTOs carry groupId/authorId properties whose names partially
     * match the entity's Long id, which would make name-based mapping
     * ambiguous (and it would have nothing sensible to do with the media list).
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

    /**
     * Checks all uploads against the media rules first and stores them only
     * when every one passes, so a rejected upload leaves nothing on disk.
     * A video (always alone) goes through the video service, which may queue
     * it for conversion.
     * @param files the uploads, possibly null
     * @return the stored file ids, in upload order
     */
    private List<String> storeMedia(List<MultipartFile> files) throws IOException {
        List<PublicationMediaType> types = PublicationMediaRules.check(files);
        List<String> mediaIds = new ArrayList<>();
        try {
            for (int i = 0; i < types.size(); i++) {
                PublicationMediaType type = types.get(i);
                mediaIds.add(type.isVideo()
                        ? videoService.accept(files.get(i), type)
                        : fileStorageService.storeFile(files.get(i), type.extension()));
            }
        } catch (IOException | RuntimeException e) {
            mediaIds.forEach(fileStorageService::deleteFileById);
            throw e;
        }
        return mediaIds;
    }

    /**
     * Deletes a publication together with its whole comment subtree, however
     * deep. The rows are the database's job: the stored procedure
     * delete_thread (migration V13) removes the tree and returns the media
     * file ids of what it removed. The files on disk are the part the
     * database cannot remove; they go only after the transaction commits, as
     * a rollback must never leave rows pointing at already-deleted files.
     * Videos still waiting to be converted leave the queue, and their
     * uploads are deleted with the rest.
     * @param rootId id of the publication or comment to delete
     */
    @Override
    @Transactional
    public void deletePublication(Long rootId) {
        List<String> mediaIds = commentsRepository.deleteThread(rootId);
        List<String> fileIds = Stream.concat(mediaIds.stream(), videoService.discard(mediaIds).stream()).toList();

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

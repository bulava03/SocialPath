package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.Publication;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.DelComment;
import com.example.SocialPath.extraClasses.NewComment;
import com.example.SocialPath.extraClasses.NewPublication;
import com.example.SocialPath.extraClasses.PublicationPresentable;
import com.example.SocialPath.repository.CommentsRepository;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.CommentsService;
import com.example.SocialPath.service.FileStorageService;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommentsServiceImpl implements CommentsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private CommentsRepository commentsRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public void addNewUserPublication(NewPublication newPublication) throws IOException {
        Publication publication = modelMapper.map(newPublication, Publication.class);

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
        if (commentIds == null) {
            return null;
        }

        List<PublicationPresentable> publications = new ArrayList<PublicationPresentable>();
        for (ObjectId element : commentIds
        ) {
            Publication publication = commentsRepository.findById(element).orElse(null);
            User author = userRepository.findByLogin(publication.getAuthorId());

            List<PublicationPresentable> publicationComments;
            if (publication.getComments() != null) {
                publicationComments = getPublications(publication.getComments());
            } else {
                publicationComments = null;
            }

            String authorAvatar;
            try {
                GridFsResource file = fileStorageService.getFileById(author.getImageId());
                authorAvatar = fileStorageService.convertGridFsFileToBase64(file);
            } catch (Exception ex) {
                authorAvatar = null;
            }

            boolean isVideo = false;

            List<String> media = new ArrayList<>();
            if (publication.getMedia() != null) {
                for (String elem : publication.getMedia()) {
                    GridFsResource resource = fileStorageService.getFileById(elem);
                    isVideo = fileStorageService.isVideo(elem);
                    media.add(fileStorageService.convertGridFsFileToBase64(resource));
                }
            }

            PublicationPresentable toAdd = new PublicationPresentable(
                    publication.getId(),
                    author.getLogin(),
                    author.getFirstName() + " " + author.getLastName(),
                    authorAvatar,
                    publication.getText(),
                    publication.getCreatedAt(),
                    media,
                    isVideo,
                    publicationComments
            );
            publications.add(toAdd);
        }
        return publications;
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

        List<PublicationPresentable> publications = new ArrayList<PublicationPresentable>();
        for (ObjectId element : ids
        ) {
            Publication publication = commentsRepository.findById(element).orElse(null);
            User author = userRepository.findByLogin(publication.getAuthorId());

            List<PublicationPresentable> publicationComments;
            if (publication.getComments() != null) {
                publicationComments = getPublications(publication.getComments());
            } else {
                publicationComments = null;
            }

            String authorAvatar;
            try {
                GridFsResource file = fileStorageService.getFileById(author.getImageId());
                authorAvatar = fileStorageService.convertGridFsFileToBase64(file);
            } catch (Exception ex) {
                authorAvatar = null;
            }

            boolean isVideo = false;

            List<String> media = new ArrayList<>();
            if (publication.getMedia() != null) {
                for (String elem : publication.getMedia()) {
                    GridFsResource resource = fileStorageService.getFileById(elem);
                    isVideo = fileStorageService.isVideo(elem);
                    media.add(fileStorageService.convertGridFsFileToBase64(resource));
                }
            }

            PublicationPresentable toAdd = new PublicationPresentable(
                    publication.getId(),
                    author.getLogin(),
                    author.getFirstName() + " " + author.getLastName(),
                    authorAvatar,
                    publication.getText(),
                    publication.getCreatedAt(),
                    media,
                    isVideo,
                    publicationComments
            );
            publications.add(toAdd);
        }
        return publications;
    }

    @Override
    public void addNewComment(NewComment newComment) throws IOException {
        Publication comment = modelMapper.map(newComment, Publication.class);

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
        Publication publication = commentsRepository.findById(delComment.getIdComment()).orElse(null);
        if (publication != null) {
            List<String> files = publication.getMedia();
            if (files != null) {
                for (String file : files) {
                    fileStorageService.deleteFileById(file);
                }
            }
        }

        userRepository.removePublicationFromUser(delComment.getLogin(), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

    @Override
    public void removePublicationGroup(DelComment delComment) {
        Publication publication = commentsRepository.findById(delComment.getIdComment()).orElse(null);
        if (publication != null) {
            List<String> files = publication.getMedia();
            if (files != null) {
                for (String file : files) {
                    fileStorageService.deleteFileById(file);
                }
            }
        }

        groupRepository.removePublicationFromGroup(new ObjectId(delComment.getGroupId()), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

    @Override
    public void removeComment(DelComment delComment) {
        Publication publication = commentsRepository.findById(delComment.getIdComment()).orElse(null);
        if (publication != null) {
            List<String> files = publication.getMedia();
            if (files != null) {
                for (String file : files) {
                    fileStorageService.deleteFileById(file);
                }
            }
        }

        commentsRepository.removeCommentFromPublication(new ObjectId(delComment.getIdPublication()), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

}

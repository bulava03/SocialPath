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
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public void addNewUserPublication(NewPublication newPublication) {
        Publication publication = modelMapper.map(newPublication, Publication.class);
        publication.setCreatedAt(LocalDateTime.now());
        Publication savedPublication = commentsRepository.save(publication);
        ObjectId publicationId = savedPublication.getId();
        userRepository.addPublicationToUser(publicationId, newPublication.getAuthorId());
    }

    @Override
    public void addNewGroupPublication(NewPublication newPublication) {
        Publication publication = modelMapper.map(newPublication, Publication.class);
        publication.setCreatedAt(LocalDateTime.now());
        Publication savedPublication = commentsRepository.save(publication);
        ObjectId publicationId = savedPublication.getId();
        groupRepository.addPublicationToGroup(publicationId, new ObjectId(newPublication.getGroupId()));
    }

    @Override
    public List<PublicationPresentable> loadComments(String type, String idInType) {
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
        return getPublications(ids); // !!! Не забути видалити !!!
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
    public List<PublicationPresentable> loadComments(List<ObjectId> commentIds) {
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

            PublicationPresentable toAdd = new PublicationPresentable(
                    publication.getId(),
                    author.getLogin(),
                    author.getFirstName() + " " + author.getLastName(),
                    publication.getText(),
                    publication.getCreatedAt(),
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
    public List<PublicationPresentable> getPublications(List<ObjectId> ids) {
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

            PublicationPresentable toAdd = new PublicationPresentable(
                    publication.getId(),
                    author.getLogin(),
                    author.getFirstName() + " " + author.getLastName(),
                    publication.getText(),
                    publication.getCreatedAt(),
                    publicationComments
            );
            publications.add(toAdd);
        }
        return publications;
    }

    @Override
    public void addNewComment(NewComment newComment) {
        Publication comment = modelMapper.map(newComment, Publication.class);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setAuthorId(newComment.getAuthorLogin());
        Publication savedComment = commentsRepository.save(comment);
        ObjectId commentId = savedComment.getId();
        commentsRepository.pushCommentId(commentId, newComment.getIdPublication());
    }

    @Override
    public void removePublicationUser(DelComment delComment) {
        userRepository.removePublicationFromUser(delComment.getLogin(), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

    @Override
    public void removePublicationGroup(DelComment delComment) {
        groupRepository.removePublicationFromGroup(new ObjectId(delComment.getGroupId()), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

    @Override
    public void removeComment(DelComment delComment) {
        commentsRepository.removeCommentFromPublication(new ObjectId(delComment.getIdPublication()), delComment.getIdComment());
        commentsRepository.removePublication(delComment.getIdComment());
    }

}

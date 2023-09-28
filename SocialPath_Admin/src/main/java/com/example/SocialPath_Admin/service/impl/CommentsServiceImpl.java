package com.example.SocialPath_Admin.service.impl;

import com.example.SocialPath_Admin.document.Group;
import com.example.SocialPath_Admin.document.Publication;
import com.example.SocialPath_Admin.document.User;
import com.example.SocialPath_Admin.extraClasses.PublicationPresentable;
import com.example.SocialPath_Admin.repository.CommentsRepository;
import com.example.SocialPath_Admin.repository.GroupRepository;
import com.example.SocialPath_Admin.repository.UserRepository;
import com.example.SocialPath_Admin.service.CommentsService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentsServiceImpl implements CommentsService {
    @Autowired
    private CommentsRepository commentsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;

    public Publication findById(ObjectId id) {
        return commentsRepository.findById(id).orElse(null);
    }

    @Override
    public List<PublicationPresentable> loadComments(ObjectId groupId) {
        Group group = groupRepository.findById(groupId).orElse(null);
        if (group != null) {
            return getPublications(group.getPublications());
        } else {
            return null;
        }
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
    public void removePublicationUser(String login, ObjectId idComment) {
        userRepository.removePublicationFromUser(login, idComment);
        commentsRepository.removePublication(idComment);
    }

    @Override
    public void removePublicationGroup(ObjectId groupId, ObjectId idComment) {
        groupRepository.removePublicationFromGroup(groupId, idComment);
        commentsRepository.removePublication(idComment);
    }

    @Override
    public void removeCommentUser(ObjectId idPublication, ObjectId idComment) {
        commentsRepository.removeCommentFromPublication(idPublication, idComment);
        commentsRepository.removePublication(idComment);
    }

    @Override
    public void removeCommentGroup(ObjectId idPublication, ObjectId idComment) {
        commentsRepository.removeCommentFromPublication(idPublication, idComment);
        commentsRepository.removePublication(idComment);
    }

    @Override
    public List<ObjectId> findByAuthorId(String authorId) {
        return commentsRepository.findByAuthorId(authorId);
    }

    @Override
    public void removeAllByIds(List<ObjectId> ids) {
        commentsRepository.deleteAllById(ids);
    }
}

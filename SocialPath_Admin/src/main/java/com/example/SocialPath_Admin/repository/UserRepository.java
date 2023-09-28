package com.example.SocialPath_Admin.repository;

import com.example.SocialPath_Admin.document.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepository {
    User findByLogin(String login);
    User findByLoginAndPassword(String login, String password);
    void removePublicationFromUser(String login, ObjectId id);
    void removeFromInvitations(String myId, String friendId);
    void removeFromFriends(String myId, String friendId);
    void removeFromGroups(String userId, ObjectId groupId);
    void updateBan(String login, LocalDateTime date);
    void removeGroupFromAllUsers(ObjectId group);
    void removePublicationIdsFromUser(List<ObjectId> publicationIds);
    void removeUserFromUserArrays(String objectId);
}

package com.example.SocialPath.repository;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.UserUpdate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

public interface UserRepository {
    void save(User user);
    User findByLogin(String login);
    User findByLoginAndPassword(String login, String password);
    void addPublicationToUser(ObjectId publicationId, String userId);
    void updateFieldsByLogin(String login, UserUpdate userUpdate);
    void updatePasswordByLogin(String login, UserUpdate userUpdate);
    void updateBizFieldsByLogin(String login, UserUpdate userUpdate);
    void addGroupToUser(ObjectId groupId, String login);
    void removePublicationFromUser(String login, ObjectId id);
    List<User> findMatchingUsers(String searchValue);
    List<String> findUsersFriends(String login);
    List<ObjectId> findUsersGroups(String login);
    List<User> findByLoginIn(List<String> ids);
    List<String> findUsersFriendsInvitations(String login);
    void addToFriends(String myId, String friendId);
    void inviteUser(String myId, String friendId);
    void removeFromInvitations(String myId, String friendId);
    void removeFromFriends(String myId, String friendId);
    void removeFromGroups(String userId, ObjectId groupId);
    List<ObjectId> getPublicationsIdList(String login);
    void addGroup(String login, ObjectId groupId);
    void removeGroup(String login, ObjectId groupId);
    void addSubscribe(String myId, String bizId);
    void removeSubscribe(String myId, String bizId);
    void addSubscriber(String bizId, String userId);
    void removeSubscriber(String bizId, String userId);
}

package com.example.SocialPath_Admin.service;

import com.example.SocialPath_Admin.document.User;
import org.bson.types.ObjectId;

import java.util.List;

public interface UserService {
    User findByLogin(String login);
    User findByLoginAndPassword(String login, String password);
    void removeFromInvitations(String myId, String anotherId);
    void removeFromFriends(String myId, String anotherId);
    void removeGroupFromAllUsers(ObjectId group);
    void updateBanById(String userLogin);
    void removePublicationIdsFromUser(List<ObjectId> publicationIds);
    void removeObjectFromUserArrays(String objectId);
    /*void deleteUserById(String userLogin);
    void markUserAsDeleted(String userLogin);*/
}

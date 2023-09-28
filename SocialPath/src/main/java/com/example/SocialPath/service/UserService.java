package com.example.SocialPath.service;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import org.bson.types.ObjectId;

import java.util.List;

public interface UserService {
    Object[] validateUser(User user);
    Object[] validateUserUpdate(UserUpdate user);
    User findUserByLoginAndPassword(String id, String password);
    void addUser(User user);
    User findUserById(String id);
    void updateUser(UserUpdate userUpdate);
    List<UserSearchResult> findUsersFriends(String login);
    List<GroupSearchResult> findUsersGroups(String login);
    List<UserSearchResult> findUsersFriendsInvitations(String login);
    void acceptToFriends(String myId, String anotherId);
    void inviteUser(String myId, String anotherId);
    void removeFromInvitations(String myId, String anotherId);
    void removeFromFriends(String myId, String anotherId);
}

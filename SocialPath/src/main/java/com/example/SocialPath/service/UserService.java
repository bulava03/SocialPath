package com.example.SocialPath.service;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;

public interface UserService {
    Object[] validateUser(User user);
    Object[] validateUserUpdate(UserUpdate user);
    String resolveToken(HttpServletRequest request);
    User findByLogin(String login);
    User findUserByLoginAndPassword(String id, String password);
    void addUser(User user);
    User findUserById(String id);
    void updateUser(UserUpdate userUpdate);
    void updatePassword(UserUpdate userUpdate);
    void updateBiz(UserUpdate userUpdate);
    List<UserSearchResult> findUsersFriends(String login) throws IOException;
    List<GroupSearchResult> findUsersGroups(String login) throws IOException;
    List<UserSearchResult> findUsersFriendsInvitations(String login) throws IOException;
    void acceptToFriends(String myId, String anotherId);
    void inviteUser(String myId, String anotherId);
    void removeFromInvitations(String myId, String anotherId);
    void removeFromFriends(String myId, String anotherId);
    void addGroup(String login, String  groupId);
    void removeGroup(String login, String groupId);
    void subscribe(String userId, String bizId);
    void unsubscribe(String userId, String bizId);
}

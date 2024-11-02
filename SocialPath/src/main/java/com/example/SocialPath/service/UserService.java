package com.example.SocialPath.service;

import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.io.IOException;
import java.util.List;

public interface UserService {
    Object[] validateUser(User user);
    Object[] validateUserUpdate(UserUpdate user);
    User findUserByLoginAndPassword(String id, String password);
    void addUser(User user);
    User findUserById(String id);
    void updateUser(UserUpdate userUpdate);
    List<UserSearchResult> findUsersFriends(String login) throws IOException;
    List<GroupSearchResult> findUsersGroups(String login) throws IOException;
    List<UserSearchResult> findUsersFriendsInvitations(String login) throws IOException;
    void acceptToFriends(String myId, String anotherId);
    void inviteUser(String myId, String anotherId);
    void removeFromInvitations(String myId, String anotherId);
    void removeFromFriends(String myId, String anotherId);
    void addGroup(String login, String  groupId);
    void removeGroup(String login, String groupId);
}

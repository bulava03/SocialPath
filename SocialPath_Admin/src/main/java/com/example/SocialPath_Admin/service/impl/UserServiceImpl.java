package com.example.SocialPath_Admin.service.impl;

import com.example.SocialPath_Admin.document.User;
import com.example.SocialPath_Admin.repository.GroupRepository;
import com.example.SocialPath_Admin.repository.UserRepository;
import com.example.SocialPath_Admin.service.UserService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;

    @Override
    public User findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    public User findByLoginAndPassword(String login, String password) {
        return userRepository.findByLoginAndPassword(login, password);
    }

    @Override
    public void removeFromInvitations(String myId, String anotherId) {
        userRepository.removeFromInvitations(myId, anotherId);
    }

    @Override
    public void removeFromFriends(String myId, String anotherId) {
        userRepository.removeFromFriends(myId, anotherId);
    }

    @Override
    public void removeGroupFromAllUsers(ObjectId group) {
        userRepository.removeGroupFromAllUsers(group);
    }

    @Override
    public void updateBanById(String userLogin) {
        userRepository.updateBan(userLogin, LocalDateTime.now().plusDays(10));
    }

    @Override
    public void removePublicationIdsFromUser(List<ObjectId> publicationIds) {
        userRepository.removePublicationIdsFromUser(publicationIds);
    }

    @Override
    public void removeObjectFromUserArrays(String objectId) {
        userRepository.removeUserFromUserArrays(objectId);
    }

    /*@Override
    public void deleteUserById(String userLogin) {
        userRepository.DeleteUserById(userLogin);
    }

    @Override
    public void markUserAsDeleted(String userLogin) {
        userRepository.MarkUserAsDeleted(userLogin);
    }*/

}

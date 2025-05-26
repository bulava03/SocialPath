package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.security.PasswordHasher;
import com.example.SocialPath.service.FileStorageService;
import com.example.SocialPath.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Validator validator;
    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public Object[] validateUser(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else {
            return new Object[] { true, "" };
        }
    }

    @Override
    public Object[] validateUserUpdate(UserUpdate user) {
        Set<ConstraintViolation<UserUpdate>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(ConstraintViolation::getMessage).toList();
            return new Object[] { false, errorMessages.stream().findFirst() };
        } else {
            return new Object[] { true, "" };
        }
    }

    @Override
    public String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        return token;
    }

    @Override
    public User findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    public User findUserByLoginAndPassword(String id, String password) {
        return userRepository.findByLoginAndPassword(id, password);
    }

    @Override
    public User findUserById(String id) {
        return userRepository.findByLogin(id);
    }

    @Override
    public void addUser(User user) {
        user.setPassword(PasswordHasher.hashPassword(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void updateUser(UserUpdate userUpdate) {
        userUpdate.setPassword(PasswordHasher.hashPassword(userUpdate.getPassword()));
        userRepository.updateFieldsByLogin(userUpdate.getLogin(), userUpdate);
    }

    @Override
    public void updatePassword(UserUpdate userUpdate) {
        userUpdate.setPassword(PasswordHasher.hashPassword(userUpdate.getPassword()));
        userRepository.updatePasswordByLogin(userUpdate.getLogin(), userUpdate);
    }

    @Override
    public void updateBiz(UserUpdate userUpdate) {
        userRepository.updateBizFieldsByLogin(userUpdate.getLogin(), userUpdate);
    }

    @Override
    public List<UserSearchResult> findUsersFriends(String login) throws IOException {
        List<String> usersFriends = userRepository.findUsersFriends(login);

        if (usersFriends == null) {
            return null;
        }

        List<User> usersFriendsObj = userRepository.findByLoginIn(usersFriends);

        List<UserSearchResult> usersFriendsPresentable = new ArrayList<>();
        for (User user : usersFriendsObj
             ) {
            if (!user.getLogin().equals(login)) {
                UserSearchResult toAdd = modelMapper.map(user, UserSearchResult.class);

                String file;
                if (user.getImageId() == null || user.getImageId().isEmpty()) {
                    file = null;
                } else {
                    GridFsResource resource = fileStorageService.getFileById(user.getImageId());
                    file = fileStorageService.convertGridFsFileToBase64(resource);
                }
                toAdd.setFile(file);

                toAdd.setAnotherUserLogin(user.getLogin());
                usersFriendsPresentable.add(toAdd);
            }
        }

        return usersFriendsPresentable;
    }

    @Override
    public List<GroupSearchResult> findUsersGroups(String login) throws IOException {
        List<ObjectId> userGroupsIds = userRepository.findUsersGroups(login);

        if (userGroupsIds == null) {
            return null;
        }

        List<Group> usersGroupsObj = groupRepository.findAllById(userGroupsIds);

        List<GroupSearchResult> usersGroupsPresentable = new ArrayList<>();
        for (Group group : usersGroupsObj
             ) {
            GroupSearchResult groupSearchResult = modelMapper.map(group, GroupSearchResult.class);
            groupSearchResult.setId(group.getId().toString());
            String file;
            if (group.getImageId() == null || group.getImageId().isEmpty()) {
                file = null;
            } else {
                GridFsResource resource = fileStorageService.getFileById(group.getImageId());
                file = fileStorageService.convertGridFsFileToBase64(resource);
            }
            groupSearchResult.setFile(file);
            usersGroupsPresentable.add(groupSearchResult);
        }

        return usersGroupsPresentable;
    }

    @Override
    public List<UserSearchResult> findUsersFriendsInvitations(String login) throws IOException {
        List<String> usersFriends = userRepository.findUsersFriendsInvitations(login);

        if (usersFriends == null) {
            return null;
        }

        List<User> usersFriendsObj = userRepository.findByLoginIn(usersFriends);

        List<UserSearchResult> usersFriendsPresentable = new ArrayList<>();
        for (User user : usersFriendsObj
             ) {
            if (!user.getLogin().equals(login)) {
                UserSearchResult toAdd = modelMapper.map(user, UserSearchResult.class);

                String file;
                if (user.getImageId() == null || user.getImageId().isEmpty()) {
                    file = null;
                } else {
                    GridFsResource resource = fileStorageService.getFileById(user.getImageId());
                    file = fileStorageService.convertGridFsFileToBase64(resource);
                }
                toAdd.setFile(file);

                toAdd.setAnotherUserLogin(user.getLogin());
                usersFriendsPresentable.add(toAdd);
            }
        }

        return usersFriendsPresentable;
    }

    @Override
    public void acceptToFriends(String myId, String anotherId) {
        userRepository.addToFriends(myId, anotherId);
        userRepository.addToFriends(anotherId, myId);
    }

    @Override
    public void inviteUser(String myId, String anotherId) {
        userRepository.inviteUser(myId, anotherId);
    }

    @Override
    public void removeFromInvitations(String myId, String anotherId) {
        userRepository.removeFromInvitations(myId, anotherId);
    }

    @Override
    public void removeFromFriends(String myId, String anotherId) {
        userRepository.removeFromFriends(myId, anotherId);
        userRepository.removeFromFriends(anotherId, myId);
    }

    @Override
    public void addGroup(String login, String  groupId) {
        userRepository.addGroup(login, new ObjectId(groupId));
    }

    @Override
    public void removeGroup(String login, String groupId) {
        userRepository.removeGroup(login, new ObjectId(groupId));
    }

    @Override
    public void subscribe(String userId, String bizId) {
        userRepository.addSubscribe(userId, bizId);
        userRepository.addSubscriber(bizId, userId);
    }

    @Override
    public void unsubscribe(String userId, String bizId) {
        userRepository.removeSubscribe(userId, bizId);
        userRepository.removeSubscriber(bizId, userId);
    }

}

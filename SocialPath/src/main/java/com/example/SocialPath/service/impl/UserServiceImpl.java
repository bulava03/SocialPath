package com.example.SocialPath.service.impl;

import com.example.SocialPath.document.Group;
import com.example.SocialPath.document.User;
import com.example.SocialPath.extraClasses.GroupSearchResult;
import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.extraClasses.UserUpdate;
import com.example.SocialPath.repository.GroupRepository;
import com.example.SocialPath.repository.UserRepository;
import com.example.SocialPath.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public User findUserByLoginAndPassword(String id, String password) {
        return userRepository.findByLoginAndPassword(id, password);
    }

    @Override
    public User findUserById(String id) {
        return userRepository.findByLogin(id);
    }

    @Override
    public void addUser(User user) {
        userRepository.save(user);
    }

    @Override
    public void updateUser(UserUpdate userUpdate) {
        userRepository.updateFieldsByLogin(userUpdate.getLogin(), userUpdate);
    }

    @Override
    public List<UserSearchResult> findUsersFriends(String login) {
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
                toAdd.setAnotherUserLogin(user.getLogin());
                usersFriendsPresentable.add(toAdd);
            }
        }

        return usersFriendsPresentable;
    }

    @Override
    public List<GroupSearchResult> findUsersGroups(String login) {
        List<ObjectId> userGroupsIds = userRepository.findUsersGroups(login);

        if (userGroupsIds == null) {
            return null;
        }

        List<Group> usersGroupsObj = groupRepository.findAllById(userGroupsIds);

        List<GroupSearchResult> usersGroupsPresentable = new ArrayList<>();
        for (Group group : usersGroupsObj
             ) {
            usersGroupsPresentable.add(modelMapper.map(group, GroupSearchResult.class));
        }

        return usersGroupsPresentable;
    }

    @Override
    public List<UserSearchResult> findUsersFriendsInvitations(String login) {
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
}

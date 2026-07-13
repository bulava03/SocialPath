package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.document.Group;
import com.socialpath.document.User;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.validation.ValidationResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.socialpath.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.bson.types.ObjectId;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ModelMapper modelMapper;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ValidationResult validateUser(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            return ValidationResult.failure(violations.stream()
                    .map(ConstraintViolation::getMessage).toList());
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateUserUpdate(UserUpdate user) {
        Set<ConstraintViolation<UserUpdate>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            return ValidationResult.failure(violations.stream()
                    .map(ConstraintViolation::getMessage).toList());
        }
        return ValidationResult.success();
    }

    @Override
    public User findByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void addUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void updateUser(UserUpdate userUpdate) {
        userRepository.updateFieldsByLogin(userUpdate.getLogin(), userUpdate);
    }

    @Override
    public void updatePassword(String login, String rawPassword) {
        userRepository.updatePasswordByLogin(login, passwordEncoder.encode(rawPassword));
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

}

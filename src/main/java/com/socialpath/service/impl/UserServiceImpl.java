package com.socialpath.service.impl;

import lombok.RequiredArgsConstructor;
import com.socialpath.entity.User;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.dto.request.RegistrationForm;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.validation.DateOfBirthResolver;
import com.socialpath.validation.ValidationResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.socialpath.service.CountryService;
import com.socialpath.service.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User accounts and the social graph. Where the MongoDB edition issued
 * targeted $push/$pull updates on embedded arrays, this edition mutates the
 * mapped collections inside a transaction and lets JPA write the join tables
 * (friendships, friend_invites). Group membership is no longer mirrored on
 * the user: it is owned by group_members and queried through GroupRepository.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;
    private final CountryService countryService;

    @Override
    public ValidationResult validateRegistration(RegistrationForm form) {
        ValidationResult dateOfBirth = DateOfBirthResolver.resolve(form);
        if (!dateOfBirth.isSuccess()) {
            return dateOfBirth;
        }
        Set<ConstraintViolation<RegistrationForm>> violations = validator.validate(form);

        if (!violations.isEmpty()) {
            return ValidationResult.failure(violations.stream()
                    .map(ConstraintViolation::getMessage).toList());
        }
        if (!countryService.isValidCode(form.getCountryCode())) {
            return ValidationResult.failure("Unknown country.");
        }
        return ValidationResult.success();
    }

    @Override
    public ValidationResult validateUserUpdate(UserUpdate user) {
        ValidationResult dateOfBirth = DateOfBirthResolver.resolve(user);
        if (!dateOfBirth.isSuccess()) {
            return dateOfBirth;
        }
        Set<ConstraintViolation<UserUpdate>> violations = validator.validate(user);

        if (!violations.isEmpty()) {
            return ValidationResult.failure(violations.stream()
                    .map(ConstraintViolation::getMessage).toList());
        }
        if (!countryService.isValidCode(user.getCountryCode())) {
            return ValidationResult.failure("Unknown country.");
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
    @Transactional
    public void updateUser(UserUpdate userUpdate) {
        User user = userRepository.findByLogin(userUpdate.getLogin());
        if (user == null) {
            return;
        }
        user.setFirstName(userUpdate.getFirstName());
        user.setLastName(userUpdate.getLastName());
        user.setEmail(userUpdate.getEmail());
        user.setPhoneNumber(userUpdate.getPhoneNumber());
        user.setDateOfBirth(userUpdate.getDateOfBirth());
        user.setCountry(countryService.resolve(userUpdate.getCountryCode()));
        user.setRegion(userUpdate.getRegion());
        user.setCity(userUpdate.getCity());
        user.setEducation(userUpdate.getEducation());
        user.setWorkplace(userUpdate.getWorkplace());
        user.setImageId(userUpdate.getImageId());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updatePassword(String login, String rawPassword) {
        User user = userRepository.findByLogin(login);
        if (user == null) {
            return;
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    @Override
    public List<UserSearchResult> findUsersFriends(String login) throws IOException {
        User user = userRepository.findByLogin(login);
        List<String> usersFriends = user != null ? user.getFriends() : null;

        if (usersFriends == null) {
            return null;
        }

        return toSearchResults(usersFriends, login);
    }

    @Override
    public List<GroupSearchResult> findUsersGroups(String login) throws IOException {
        return groupRepository.findByMembersContains(login).stream()
                .map(GroupSearchResult::from)
                .toList();
    }

    @Override
    public List<UserSearchResult> findUsersFriendsInvitations(String login) throws IOException {
        User user = userRepository.findByLogin(login);
        List<String> inviters = user != null ? user.getFriendInvites() : null;

        if (inviters == null) {
            return null;
        }

        return toSearchResults(inviters, login);
    }

    /**
     * Loads the given users for a list view, leaving out the viewer.
     * @param logins logins to load
     * @param viewerLogin the current user's login
     * @return the users as list items
     */
    private List<UserSearchResult> toSearchResults(List<String> logins, String viewerLogin) {
        return userRepository.findByLoginIn(logins).stream()
                .filter(user -> !user.getLogin().equals(viewerLogin))
                .map(UserSearchResult::from)
                .toList();
    }

    @Override
    @Transactional
    public void acceptToFriends(String myId, String anotherId) {
        addFriendOneWay(myId, anotherId);
        addFriendOneWay(anotherId, myId);
    }

    @Override
    @Transactional
    public void inviteUser(String myId, String anotherId) {
        User invited = userRepository.findByLogin(anotherId);
        if (invited == null) {
            return;
        }
        if (invited.getFriendInvites() == null) {
            invited.setFriendInvites(new ArrayList<>());
        }
        if (!invited.getFriendInvites().contains(myId)) {
            invited.getFriendInvites().add(myId);
            userRepository.save(invited);
        }
    }

    @Override
    @Transactional
    public void removeFromInvitations(String myId, String anotherId) {
        User user = userRepository.findByLogin(myId);
        if (user == null || user.getFriendInvites() == null) {
            return;
        }
        user.getFriendInvites().remove(anotherId);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeFromFriends(String myId, String anotherId) {
        removeFriendOneWay(myId, anotherId);
        removeFriendOneWay(anotherId, myId);
    }

    private void addFriendOneWay(String owner, String friend) {
        User user = userRepository.findByLogin(owner);
        if (user == null) {
            return;
        }
        if (user.getFriends() == null) {
            user.setFriends(new ArrayList<>());
        }
        if (!user.getFriends().contains(friend)) {
            user.getFriends().add(friend);
            userRepository.save(user);
        }
    }

    private void removeFriendOneWay(String owner, String friend) {
        User user = userRepository.findByLogin(owner);
        if (user == null || user.getFriends() == null) {
            return;
        }
        user.getFriends().remove(friend);
        userRepository.save(user);
    }
}

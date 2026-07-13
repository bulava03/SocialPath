package com.socialpath.service;

import com.socialpath.document.User;
import com.socialpath.dto.response.GroupSearchResult;
import com.socialpath.dto.response.UserSearchResult;
import com.socialpath.dto.request.UserUpdate;
import com.socialpath.validation.ValidationResult;

import java.io.IOException;
import java.util.List;

/**
 * User accounts and the social graph around them: profile data, friendships
 * (including pending invitations) and group membership references.
 */
public interface UserService {

    /**
     * Validates a new user against its Bean Validation constraints.
     * @param user the user to validate
     * @return success, or a failure carrying the first violation message
     */
    ValidationResult validateUser(User user);

    /**
     * Validates a profile update against its Bean Validation constraints.
     * @param user the update to validate
     * @return success, or a failure carrying the first violation message
     */
    ValidationResult validateUserUpdate(UserUpdate user);

    /**
     * Looks up a user by login, which is also the document id.
     * @param login the login to look up
     * @return the user, or null if none exists
     */
    User findByLogin(String login);

    /**
     * @return all users (used by search and administrative flows)
     */
    List<User> findAll();

    /**
     * Persists a new user, hashing the password before storing.
     * @param user the user to create
     */
    void addUser(User user);

    /**
     * Applies a profile update to the stored user.
     * @param userUpdate the fields to update, keyed by the user's login
     */
    void updateUser(UserUpdate userUpdate);

    /**
     * Sets a new password, hashing the raw value before storing.
     * @param login the user whose password changes
     * @param rawPassword the new plaintext password to hash and store
     */
    void updatePassword(String login, String rawPassword);

    /**
     * Resolves a user's friends into their presentable form.
     * @param login the user whose friends are wanted
     * @return the friends as search results
     */
    List<UserSearchResult> findUsersFriends(String login) throws IOException;

    /**
     * Resolves the groups a user belongs to into presentable form.
     * @param login the user whose groups are wanted
     * @return the groups as search results
     */
    List<GroupSearchResult> findUsersGroups(String login) throws IOException;

    /**
     * Resolves a user's pending incoming friend invitations.
     * @param login the user whose invitations are wanted
     * @return the inviting users as search results
     */
    List<UserSearchResult> findUsersFriendsInvitations(String login) throws IOException;

    /**
     * Establishes a friendship between two users (both directions).
     * @param myId one side of the new friendship
     * @param anotherId the other side
     */
    void acceptToFriends(String myId, String anotherId);

    /**
     * Records a friend invitation from one user to another.
     * @param myId the inviting user
     * @param anotherId the invited user
     */
    void inviteUser(String myId, String anotherId);

    /**
     * Removes a pending friend invitation.
     * @param myId the user whose invitation list is edited
     * @param anotherId the other user in the invitation
     */
    void removeFromInvitations(String myId, String anotherId);

    /**
     * Dissolves a friendship between two users (both directions).
     * @param myId one side of the friendship
     * @param anotherId the other side
     */
    void removeFromFriends(String myId, String anotherId);

    /**
     * Adds a group reference to a user's membership list.
     * @param login the member
     * @param groupId the group joined
     */
    void addGroup(String login, String groupId);

    /**
     * Removes a group reference from a user's membership list.
     * @param login the member
     * @param groupId the group left
     */
    void removeGroup(String login, String groupId);
}

package com.example.SocialPath;

import com.example.SocialPath.document.User;
import com.example.SocialPath.helper.CheckHelper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckHelperTest {

    @Test
    void nullOrBannedCheck_WithNullUser_ReturnsErrorMessage() {
        // Arrange
        User user = null;

        // Act
        String result = CheckHelper.nullOrBannedCheck(user);

        // Assert
        assertEquals("Неправильний логін або пароль", result);
    }

    @Test
    void nullOrBannedCheck_WithBannedUser_ReturnsErrorMessage() {
        // Arrange
        User user = new User();
        user.setBan(LocalDateTime.now().plusDays(1)); // Banned for 1 day

        // Act
        String result = CheckHelper.nullOrBannedCheck(user);

        // Assert
        assertEquals("Цей акаунт заблоковано", result);
    }

    @Test
    void nullOrBannedCheck_WithActiveBannedInPast_ReturnsEmptyString() {
        // Arrange
        User user = new User();
        user.setBan(LocalDateTime.now().minusDays(1)); // Ban expired

        // Act
        String result = CheckHelper.nullOrBannedCheck(user);

        // Assert
        assertEquals("", result);
    }

    @Test
    void nullOrBannedCheck_WithValidUser_ReturnsEmptyString() {
        // Arrange
        User user = new User();
        user.setBan(null);

        // Act
        String result = CheckHelper.nullOrBannedCheck(user);

        // Assert
        assertEquals("", result);
    }

    @Test
    void inRequestsCheck_WhenNoInvites_ReturnsZero() {
        // Arrange
        User user = new User();
        User myUser = new User();
        String login = "testLogin";
        String anotherUserLogin = "anotherLogin";

        // Act
        int result = CheckHelper.inRequestsCheck(user, myUser, login, anotherUserLogin);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void inRequestsCheck_WhenInviteExists_ReturnsOne() {
        // Arrange
        User user = new User();
        User myUser = new User();
        String login = "testLogin";
        String anotherUserLogin = "anotherLogin";

        List<String> invites = new ArrayList<>();
        invites.add(login);
        user.setFriendInvites(invites);

        // Act
        int result = CheckHelper.inRequestsCheck(user, myUser, login, anotherUserLogin);

        // Assert
        assertEquals(1, result);
    }

    @Test
    void inRequestsCheck_WhenReverseInviteExists_ReturnsTwo() {
        // Arrange
        User user = new User();
        User myUser = new User();
        String login = "testLogin";
        String anotherUserLogin = "anotherLogin";

        List<String> userInvites = new ArrayList<>();
        userInvites.add(login);
        user.setFriendInvites(userInvites);

        List<String> myUserInvites = new ArrayList<>();
        myUserInvites.add(anotherUserLogin);
        myUser.setFriendInvites(myUserInvites);

        // Act
        int result = CheckHelper.inRequestsCheck(user, myUser, login, anotherUserLogin);

        // Assert
        assertEquals(2, result);
    }

    @Test
    void inRequestsCheck_WhenAlreadyFriends_ReturnsThree() {
        // Arrange
        User user = new User();
        User myUser = new User();
        String login = "testLogin";
        String anotherUserLogin = "anotherLogin";

        List<String> friends = new ArrayList<>();
        friends.add(anotherUserLogin);
        myUser.setFriends(friends);

        // Act
        int result = CheckHelper.inRequestsCheck(user, myUser, login, anotherUserLogin);

        // Assert
        assertEquals(3, result);
    }
}

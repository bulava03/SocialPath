package com.socialpath;

import com.socialpath.entity.User;
import com.socialpath.helper.CheckHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckHelperTest {

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

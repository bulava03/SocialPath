package com.socialpath.helper;

import com.socialpath.entity.User;


public class CheckHelper {

    public static int inRequestsCheck(User user, User myUser, String login, String anotherUserLogin) {
        if (myUser.getFriends().contains(anotherUserLogin)) {
            return 3;
        }
        if (myUser.getFriendInvites().contains(anotherUserLogin)) {
            return 2;
        }
        if (user.getFriendInvites().contains(login)) {
            return 1;
        }
        return 0;
    }

}

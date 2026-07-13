package com.socialpath.helper;

import com.socialpath.document.User;


public class CheckHelper {

    public static int inRequestsCheck(User user, User myUser, String login, String anotherUserLogin) {
        int inRequests;

        if (user.getFriendInvites() == null || user.getFriendInvites().isEmpty()) {
            inRequests = 0;
        } else if (!user.getFriendInvites().contains(login)) {
            inRequests = 0;
        } else {
            inRequests = 1;
        }

        if (myUser.getFriendInvites() != null) {
            if (myUser.getFriendInvites().contains(anotherUserLogin)) {
                inRequests = 2;
            }
        }

        if (myUser.getFriends() != null) {
            if (myUser.getFriends().contains(anotherUserLogin)) {
                inRequests = 3;
            }
        }

        return inRequests;
    }

}

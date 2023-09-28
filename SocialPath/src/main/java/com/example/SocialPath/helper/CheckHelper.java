package com.example.SocialPath.helper;

import com.example.SocialPath.document.User;

import java.time.LocalDateTime;

public class CheckHelper {

    public static String nullOrBannedCheck(User user) {
        if (user == null) {
            return "Неправильний логін або пароль";
        } else if (user.getBan() != null && user.getBan().isAfter(LocalDateTime.now())) {
            return "Цей акаунт заблоковано";
        }
        return "";
    }

    public static int inRequestsCheck(User user, User myUser, String login, String anotherUserLogin) {
        int inRequests;

        if (user.getFriendInvites() == null || user.getFriendInvites().size() == 0) {
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

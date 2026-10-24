package com.socialpath.dto.response;

import com.socialpath.entity.User;

/**
 * How the viewer of a profile relates to its owner; picks the friend buttons
 * shown on another user's page.
 */
public enum FriendshipStatus {
    /** No friendship and no pending request either way. */
    NONE,
    /** The viewer has asked the other user and waits for an answer. */
    REQUEST_SENT,
    /** The other user has asked the viewer. */
    REQUEST_RECEIVED,
    /** Already friends. */
    FRIENDS;

    /**
     * Works out the relation from the viewer's side.
     * @param viewer the signed-in user
     * @param other the user whose page is open
     * @return the relation; friendship wins over any request still stored
     */
    public static FriendshipStatus between(User viewer, User other) {
        if (viewer.getFriends().contains(other.getLogin())) {
            return FRIENDS;
        }
        if (viewer.getFriendInvites().contains(other.getLogin())) {
            return REQUEST_RECEIVED;
        }
        if (other.getFriendInvites().contains(viewer.getLogin())) {
            return REQUEST_SENT;
        }
        return NONE;
    }
}

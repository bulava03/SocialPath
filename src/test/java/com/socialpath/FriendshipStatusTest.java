package com.socialpath;

import com.socialpath.dto.response.FriendshipStatus;
import com.socialpath.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FriendshipStatusTest {

    private User viewer;
    private User other;

    @BeforeEach
    void setUp() {
        viewer = new User();
        viewer.setLogin("viewer");
        other = new User();
        other.setLogin("other");
    }

    @Test
    void between_Strangers_IsNone() {
        assertEquals(FriendshipStatus.NONE, FriendshipStatus.between(viewer, other));
    }

    @Test
    void between_ViewerAsked_IsRequestSent() {
        other.getFriendInvites().add("viewer");

        assertEquals(FriendshipStatus.REQUEST_SENT, FriendshipStatus.between(viewer, other));
    }

    @Test
    void between_OtherAsked_IsRequestReceived() {
        viewer.getFriendInvites().add("other");

        assertEquals(FriendshipStatus.REQUEST_RECEIVED, FriendshipStatus.between(viewer, other));
    }

    @Test
    void between_BothAsked_TheIncomingRequestCanBeAccepted() {
        viewer.getFriendInvites().add("other");
        other.getFriendInvites().add("viewer");

        assertEquals(FriendshipStatus.REQUEST_RECEIVED, FriendshipStatus.between(viewer, other));
    }

    @Test
    void between_Friends_IsFriends() {
        viewer.getFriends().add("other");
        other.getFriendInvites().add("viewer");

        assertEquals(FriendshipStatus.FRIENDS, FriendshipStatus.between(viewer, other));
    }
}

package com.socialpath;

import com.socialpath.dto.response.FriendshipStatus;
import com.socialpath.entity.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FriendshipStatusTest {

    /**
     * What the viewer sees on the other user's page, from who asked whom
     * (an invite is kept on the invited user) and whether they are friends.
     */
    @ParameterizedTest(name = "viewer asked: {0}, other asked: {1}, friends: {2} -> {3}")
    @CsvSource({
        "false, false, false, NONE",
        "true,  false, false, REQUEST_SENT",
        "false, true,  false, REQUEST_RECEIVED",
        // Both asked: the incoming request can simply be accepted.
        "true,  true,  false, REQUEST_RECEIVED",
        // A leftover invite does not matter once they are friends.
        "true,  false, true,  FRIENDS",
    })
    void between(boolean viewerAsked, boolean otherAsked, boolean friends, FriendshipStatus expected) {
        User viewer = new User();
        viewer.setLogin("viewer");
        User other = new User();
        other.setLogin("other");
        if (viewerAsked) {
            other.getFriendInvites().add("viewer");
        }
        if (otherAsked) {
            viewer.getFriendInvites().add("other");
        }
        if (friends) {
            viewer.getFriends().add("other");
        }

        assertEquals(expected, FriendshipStatus.between(viewer, other));
    }
}

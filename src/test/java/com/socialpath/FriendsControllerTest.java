package com.socialpath;

import com.socialpath.entity.User;
import com.socialpath.exception.ResourceNotFoundException;
import com.socialpath.service.UserService;
import com.socialpath.web.rest.FriendsController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The friend buttons rely on these outcomes to know which button to show next. */
class FriendsControllerTest {

    private UserService userService;
    private FriendsController controller;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new FriendsController(userService);
        alice = user("alice");
        bob = user("bob");
    }

    private User user(String login) {
        User user = new User();
        user.setLogin(login);
        when(userService.findByLogin(login)).thenReturn(user);
        return user;
    }

    @Test
    void inviteToFriends_Stranger_IsInvited() {
        assertEquals("INVITED", controller.inviteToFriends(alice, "bob").result());
        verify(userService).inviteUser("alice", "bob");
    }

    @Test
    void inviteToFriends_AlreadyFriends_SendsNothing() {
        bob.getFriends().add("alice");

        assertEquals("ALREADY_FRIENDS", controller.inviteToFriends(alice, "bob").result());
        verify(userService, never()).inviteUser(any(), any());
    }

    @Test
    void inviteToFriends_AlreadyInvited_SendsNothing() {
        bob.getFriendInvites().add("alice");

        assertEquals("ALREADY_INVITED", controller.inviteToFriends(alice, "bob").result());
        verify(userService, never()).inviteUser(any(), any());
    }

    @Test
    void inviteToFriends_UnknownUser_NotFound() {
        assertThrows(ResourceNotFoundException.class, () -> controller.inviteToFriends(alice, "nobody"));
        verify(userService, never()).inviteUser(any(), any());
    }

    @Test
    void acceptToFriends_WithInvite_BecomesFriendsAndClearsTheInvite() {
        alice.getFriendInvites().add("bob");

        assertEquals("ACCEPTED", controller.acceptToFriends(alice, "bob").result());
        verify(userService).acceptToFriends("alice", "bob");
        verify(userService).removeFromInvitations("alice", "bob");
    }

    @Test
    void acceptToFriends_WithoutInvite_CannotForceAFriendship() {
        assertEquals("NO_INVITE", controller.acceptToFriends(alice, "bob").result());
        verify(userService, never()).acceptToFriends(any(), any());
    }

    @Test
    void removeInviteToFriends_PendingInvite_IsCancelled() {
        bob.getFriendInvites().add("alice");

        assertEquals("INVITE_CANCELLED", controller.removeInviteToFriends(alice, "bob").result());
        verify(userService).removeFromInvitations("bob", "alice");
    }

    @Test
    void removeInviteToFriends_NothingPending_ChangesNothing() {
        assertEquals("NO_RELATION", controller.removeInviteToFriends(alice, "bob").result());
        verify(userService, never()).removeFromInvitations(any(), any());
        verify(userService, never()).removeFromFriends(any(), any());
    }
}

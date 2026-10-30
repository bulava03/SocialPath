package com.socialpath;

import com.socialpath.entity.User;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.CountryService;
import com.socialpath.service.UserService;
import com.socialpath.service.impl.UserServiceImpl;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceFriendshipTest {

    private UserRepository userRepository;
    private UserServiceImpl service;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserServiceImpl(
                userRepository,
                mock(GroupRepository.class),
                mock(Validator.class),
                mock(PasswordEncoder.class),
                mock(CountryService.class));
        alice = user("alice");
        bob = user("bob");
    }

    private User user(String login) {
        User user = new User();
        user.setLogin(login);
        when(userRepository.findByLogin(login)).thenReturn(user);
        return user;
    }

    @Test
    void inviteUser_Themselves_IsRefusedAndNothingIsStored() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.inviteUser("alice", "alice"));

        assertEquals(UserService.SELF_FRIENDSHIP, error.getMessage());
        assertEquals(List.of(), alice.getFriendInvites());
        verify(userRepository, never()).save(any());
    }

    @Test
    void acceptToFriends_Themselves_IsRefusedAndNothingIsStored() {
        assertThrows(IllegalArgumentException.class, () -> service.acceptToFriends("alice", "alice"));

        assertEquals(List.of(), alice.getFriends());
        verify(userRepository, never()).save(any());
    }

    @Test
    void inviteUser_PutsTheInviteOnTheInvitedUser() {
        service.inviteUser("alice", "bob");

        assertEquals(List.of("alice"), bob.getFriendInvites());
        assertEquals(List.of(), alice.getFriendInvites());
    }

    @Test
    void inviteUser_Twice_KeepsASingleInvite() {
        service.inviteUser("alice", "bob");
        service.inviteUser("alice", "bob");

        assertEquals(List.of("alice"), bob.getFriendInvites());
    }

    @Test
    void acceptToFriends_LinksBothUsers() {
        service.acceptToFriends("bob", "alice");

        assertEquals(List.of("alice"), bob.getFriends());
        assertEquals(List.of("bob"), alice.getFriends());
    }
}

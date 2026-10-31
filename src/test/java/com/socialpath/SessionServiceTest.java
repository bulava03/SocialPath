package com.socialpath;

import com.socialpath.dto.response.SessionView;
import com.socialpath.entity.UserSession;
import com.socialpath.repository.UserSessionRepository;
import com.socialpath.service.impl.SessionServiceImpl;
import com.socialpath.session.IpLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

    private static final Duration TIMEOUT = Duration.ofDays(7);

    private UserSessionRepository repository;
    private IpLocator ipLocator;
    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(UserSessionRepository.class);
        ipLocator = mock(IpLocator.class);
        service = new SessionServiceImpl(repository, ipLocator, TIMEOUT);
        when(repository.save(any(UserSession.class))).thenAnswer(call -> call.getArgument(0));
    }

    /** A session of anna, last used the given time ago. */
    private static UserSession usedAgo(Duration ago) {
        LocalDateTime lastSeen = LocalDateTime.now().minus(ago);
        return new UserSession("anna", "203.0.113.7", "Firefox/131.0", "Somewhere", lastSeen, lastSeen.plus(TIMEOUT));
    }

    @Test
    void open_RecordsTheDeviceAndExpiresAfterTheTimeout() {
        when(ipLocator.locate("203.0.113.7")).thenReturn("Northtown, Exampleland");

        UserSession session = service.open("anna", "203.0.113.7", "Firefox/131.0");

        assertEquals("anna", session.getUserLogin());
        assertEquals("Northtown, Exampleland", session.getLocation());
        assertEquals("Firefox/131.0", session.getUserAgent());
        assertEquals(TIMEOUT, Duration.between(session.getCreatedAt(), session.getExpiresAt()));
        verify(repository).save(session);
    }

    @Test
    void open_TwoSessions_GetDifferentIds() {
        assertFalse(service.open("anna", "203.0.113.7", null).getId()
                .equals(service.open("anna", "203.0.113.7", null).getId()));
    }

    @Test
    void findActive_OnlyALiveSessionOfTheSameUser() {
        UserSession live = usedAgo(Duration.ofHours(1));
        UserSession expired = usedAgo(TIMEOUT.plusMinutes(1));
        when(repository.findById(live.getId())).thenReturn(Optional.of(live));
        when(repository.findById(expired.getId())).thenReturn(Optional.of(expired));

        assertEquals(Optional.of(live), service.findActive(live.getId(), "anna"));
        // A token of one user can never be used with another user's session.
        assertTrue(service.findActive(live.getId(), "ben").isEmpty());
        assertTrue(service.findActive(expired.getId(), "anna").isEmpty());
        assertTrue(service.findActive("ended-session", "anna").isEmpty());
        assertTrue(service.findActive(null, "anna").isEmpty());
    }

    @Test
    void touch_RecentlyUsed_WritesNothing() {
        UserSession session = usedAgo(Duration.ofMinutes(1));
        LocalDateTime expiresAt = session.getExpiresAt();

        assertFalse(service.touch(session, "203.0.113.7"));

        assertEquals(expiresAt, session.getExpiresAt());
        verify(repository, never()).save(any());
    }

    @Test
    void touch_AfterTheRefreshInterval_ExtendsTheSession() {
        UserSession session = usedAgo(Duration.ofMinutes(6));

        assertTrue(service.touch(session, "203.0.113.7"));

        assertTrue(session.getLastSeenAt().isAfter(LocalDateTime.now().minusSeconds(5)));
        assertEquals(TIMEOUT, Duration.between(session.getLastSeenAt(), session.getExpiresAt()));
        assertEquals("Somewhere", session.getLocation());
        verify(repository).save(session);
        verify(ipLocator, never()).locate(any());
    }

    @Test
    void touch_FromANewAddress_UpdatesTheLocation() {
        UserSession session = usedAgo(Duration.ofMinutes(6));
        when(ipLocator.locate("198.51.100.4")).thenReturn("Southtown, Exampleland");

        service.touch(session, "198.51.100.4");

        assertEquals("198.51.100.4", session.getIp());
        assertEquals("Southtown, Exampleland", session.getLocation());
    }

    @Test
    void list_PutsTheCurrentSessionFirst() {
        UserSession recent = usedAgo(Duration.ofMinutes(1));
        UserSession current = usedAgo(Duration.ofHours(2));
        when(repository.findByUserLoginOrderByLastSeenAtDesc("anna")).thenReturn(List.of(recent, current));

        List<SessionView> sessions = service.list("anna", current.getId());

        assertEquals(List.of(current.getId(), recent.getId()), sessions.stream().map(SessionView::id).toList());
        assertTrue(sessions.get(0).current());
        assertFalse(sessions.get(1).current());
        assertEquals("Firefox", sessions.get(0).browser());
    }
}

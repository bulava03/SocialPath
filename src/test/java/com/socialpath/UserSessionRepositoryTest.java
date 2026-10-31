package com.socialpath;

import com.socialpath.entity.UserSession;
import com.socialpath.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The deletes that end sessions, on the real schema. */
class UserSessionRepositoryTest extends MySqlRepositoryTest {

    @Autowired
    private UserSessionRepository repository;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void users() {
        user("anna");
        user("ben");
    }

    private UserSession session(String login, LocalDateTime expiresAt) {
        return repository.saveAndFlush(new UserSession(login, "203.0.113.7", "Firefox/131.0", null,
                now.minusHours(1), expiresAt));
    }

    private List<String> idsOf(String login) {
        return repository.findByUserLoginOrderByLastSeenAtDesc(login).stream().map(UserSession::getId).toList();
    }

    @Test
    void deleteByIdAndUserLogin_EndsOnlyTheUsersOwnSession() {
        UserSession bens = session("ben", now.plusDays(1));

        assertEquals(0, repository.deleteByIdAndUserLogin(bens.getId(), "anna"));
        assertEquals(1, repository.deleteByIdAndUserLogin(bens.getId(), "ben"));
        assertTrue(idsOf("ben").isEmpty());
    }

    @Test
    void deleteOthers_KeepsTheCurrentSessionAndOtherUsers() {
        UserSession current = session("anna", now.plusDays(1));
        session("anna", now.plusDays(1));
        session("anna", now.plusDays(1));
        UserSession bens = session("ben", now.plusDays(1));

        assertEquals(2, repository.deleteOthers("anna", current.getId()));

        assertEquals(List.of(current.getId()), idsOf("anna"));
        assertEquals(List.of(bens.getId()), idsOf("ben"));
    }

    @Test
    void deleteExpired_RemovesOnlySessionsPastTheirExpiry() {
        session("anna", now.minusMinutes(1));
        UserSession live = session("anna", now.plusMinutes(1));

        assertEquals(1, repository.deleteExpired(now));

        assertEquals(List.of(live.getId()), idsOf("anna"));
    }

    @Test
    void deletingTheUser_DeletesTheirSessions() {
        session("anna", now.plusDays(1));

        jdbc.update("DELETE FROM users WHERE login = 'anna'");

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_sessions", Integer.class));
    }
}

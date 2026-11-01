package com.socialpath;

import com.socialpath.entity.PhoneCode;
import com.socialpath.repository.PhoneCodeRepository;
import com.socialpath.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The schema rules behind signing in by phone: one account per confirmed
 * number, and unconfirmed registrations cleaned up with their codes.
 */
class PhoneVerificationRepositoryTest extends MySqlRepositoryTest {

    private static final String PHONE = "+12015550123";

    @Autowired
    private UserRepository users;

    @Autowired
    private PhoneCodeRepository codes;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void twoAccountsWithOneNumber() {
        // MySqlRepositoryTest.user gives everyone the same, unconfirmed number.
        user("anna");
        user("ben");
    }

    private void confirm(String login) {
        jdbc.update("UPDATE users SET phone_verified = TRUE WHERE login = ?", login);
    }

    @Test
    void aNumberMayBeTypedByManyButConfirmedByOne() {
        confirm("anna");

        assertThrows(DataIntegrityViolationException.class, () -> confirm("ben"));
        assertEquals("anna", users.findByVerifiedPhone(PHONE).getLogin());
    }

    @Test
    void anUnconfirmedNumberSignsNobodyIn() {
        assertNull(users.findByVerifiedPhone(PHONE));
        assertFalse(users.isPhoneVerifiedByOther(PHONE, null));
    }

    @Test
    void isPhoneVerifiedByOther_LeavesOutTheOwnAccount() {
        confirm("anna");

        assertFalse(users.isPhoneVerifiedByOther(PHONE, "anna"));
        assertTrue(users.isPhoneVerifiedByOther(PHONE, "ben"));
        assertTrue(users.isPhoneVerifiedByOther(PHONE, null));
    }

    @Test
    void changingAConfirmedNumberFreesIt() {
        confirm("anna");
        jdbc.update("UPDATE users SET phone = '+12015550199', phone_verified = FALSE WHERE login = 'anna'");

        confirm("ben");
        assertEquals("ben", users.findByVerifiedPhone(PHONE).getLogin());
    }

    @Test
    void deletePendingBefore_RemovesOldRegistrationsWithTheirCodes() {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE users SET pending_since = ? WHERE login = 'anna'", now.minusDays(2));
        jdbc.update("UPDATE users SET pending_since = ? WHERE login = 'ben'", now.minusHours(1));
        PhoneCode code = new PhoneCode("anna");
        code.setPhone(PHONE);
        code.setCodeHash("hash");
        code.setSentAt(now.minusDays(2));
        code.setExpiresAt(now.minusDays(2).plusMinutes(10));
        codes.saveAndFlush(code);

        assertEquals(1, users.deletePendingBefore(now.minusDays(1)));
        // The code went by ON DELETE CASCADE, behind the persistence context's back.
        entityManager.clear();

        assertNull(users.findByLogin("anna"));
        assertTrue(codes.findById(code.getId()).isEmpty());
        assertEquals("ben", users.findByLogin("ben").getLogin());
    }

    @Test
    void searchLeavesOutPendingRegistrations() {
        jdbc.update("UPDATE users SET pending_since = ? WHERE login = 'anna'", LocalDateTime.now());

        assertEquals(1, users.findMatchingUsers("example.com").size());
    }
}

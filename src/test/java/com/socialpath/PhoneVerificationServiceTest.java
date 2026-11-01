package com.socialpath;

import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;
import com.socialpath.repository.PhoneCodeRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.PhoneVerificationService.Outcome;
import com.socialpath.service.impl.PhoneVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.socialpath.service.PhoneVerificationService.MAX_ATTEMPTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sending and checking phone codes: what goes out, when another may be
 * sent, and every way a code stops working.
 */
class PhoneVerificationServiceTest {

    private static final String PHONE = "+12015550123";

    /** A clock the tests move forward. */
    private static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-01T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private record Sms(String phone, String text) {
    }

    private final TestClock clock = new TestClock();
    private final List<Sms> sent = new ArrayList<>();
    private PhoneCodeRepository codeRepository;
    private UserRepository userRepository;
    private PhoneVerificationServiceImpl service;
    private User anna;
    /** The row the repository "stores". */
    private PhoneCode stored;

    @BeforeEach
    void setUp() {
        codeRepository = mock(PhoneCodeRepository.class);
        userRepository = mock(UserRepository.class);
        service = new PhoneVerificationServiceImpl(codeRepository, userRepository,
                (phone, text) -> sent.add(new Sms(phone, text)), new BCryptPasswordEncoder(4), clock);

        anna = new User();
        anna.setLogin("anna");
        anna.setPhone(PHONE);
        anna.setPendingSince(LocalDateTime.now(clock));
        when(userRepository.findByLogin("anna")).thenReturn(anna);
        when(codeRepository.findByUserLogin("anna")).thenAnswer(invocation -> Optional.ofNullable(stored));
        when(codeRepository.save(any(PhoneCode.class))).thenAnswer(invocation -> {
            stored = invocation.getArgument(0);
            return stored;
        });
    }

    /** The digits of the last message, read the way the user would. */
    private String lastCode() {
        Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(sent.getLast().text());
        assertTrue(matcher.find(), sent.getLast().text());
        return matcher.group(1);
    }

    /** Any six digits other than the right ones. */
    private String wrongCode() {
        return lastCode().equals("000000") ? "111111" : "000000";
    }

    @Test
    void requestCode_SendsSixDigitsToTheNumberAndKeepsOnlyAHash() {
        PhoneCode code = service.requestCode(anna);

        assertEquals(1, sent.size());
        assertEquals(PHONE, sent.getFirst().phone());
        String digits = lastCode();
        assertFalse(code.getCodeHash().contains(digits));
        assertEquals(PHONE, code.getPhone());
        assertEquals(LocalDateTime.now(clock).plusMinutes(10), code.getExpiresAt());
    }

    @Test
    void requestCode_AgainTooSoon_KeepsTheCodeThatWasSent() {
        PhoneCode first = service.requestCode(anna);
        clock.advance(Duration.ofSeconds(30));

        PhoneCode second = service.requestCode(anna);

        assertEquals(1, sent.size());
        assertEquals(first.getCodeHash(), second.getCodeHash());
        assertEquals(30, service.secondsUntilResend(second));
    }

    @Test
    void requestCode_AfterTheWait_SendsANewCodeUnderTheSameId() {
        PhoneCode first = service.requestCode(anna);
        String id = first.getId();
        String firstHash = first.getCodeHash();
        clock.advance(Duration.ofSeconds(61));

        PhoneCode second = service.requestCode(anna);

        assertEquals(2, sent.size());
        assertEquals(id, second.getId());
        assertFalse(firstHash.equals(second.getCodeHash()));
        assertEquals(0, second.getAttempts());
    }

    @Test
    void confirm_RightCode_ConfirmsTheNumberAndFinishesTheRegistration() {
        PhoneCode code = service.requestCode(anna);

        // Spaces as a phone shows the code ("123 456") are fine.
        String typed = lastCode().substring(0, 3) + " " + lastCode().substring(3);
        assertEquals(Outcome.CONFIRMED, service.confirm(code, typed));

        assertTrue(anna.isPhoneVerified());
        assertNull(anna.getPendingSince());
        verify(userRepository).save(anna);
        verify(codeRepository).delete(code);
    }

    @Test
    void confirm_WrongCodes_CountDownAndThenTheCodeIsUsedUp() {
        PhoneCode code = service.requestCode(anna);
        String right = lastCode();

        for (int i = 1; i < MAX_ATTEMPTS; i++) {
            assertEquals(Outcome.WRONG_CODE, service.confirm(code, wrongCode()));
            assertEquals(i, code.getAttempts());
        }
        assertEquals(Outcome.EXPIRED, service.confirm(code, wrongCode()));
        // Not even the right code helps now: guessing would otherwise be a matter of time.
        assertEquals(Outcome.EXPIRED, service.confirm(code, right));
        assertFalse(anna.isPhoneVerified());
    }

    @Test
    void confirm_AfterTenMinutes_TheCodeHasExpired() {
        PhoneCode code = service.requestCode(anna);
        clock.advance(Duration.ofMinutes(10).plusSeconds(1));

        assertEquals(Outcome.EXPIRED, service.confirm(code, lastCode()));
        assertFalse(anna.isPhoneVerified());
    }

    @Test
    void confirm_NumberChangedSinceTheCodeWasSent_DoesNotConfirmTheNewOne() {
        PhoneCode code = service.requestCode(anna);
        anna.setPhone("+12015550199");

        assertEquals(Outcome.EXPIRED, service.confirm(code, lastCode()));
        assertFalse(anna.isPhoneVerified());
    }

    @Test
    void confirm_NumberConfirmedByAnotherAccountMeanwhile_IsRefused() {
        PhoneCode code = service.requestCode(anna);
        when(userRepository.isPhoneVerifiedByOther(PHONE, "anna")).thenReturn(true);

        assertEquals(Outcome.NUMBER_TAKEN, service.confirm(code, lastCode()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirm_LosingTheRaceForTheUniqueKey_IsRefusedTheSameWay() {
        PhoneCode code = service.requestCode(anna);
        when(userRepository.save(anna)).thenThrow(new DataIntegrityViolationException("uk_users_verified_phone"));

        assertEquals(Outcome.NUMBER_TAKEN, service.confirm(code, lastCode()));
        verify(codeRepository, never()).delete(any());
    }

    @Test
    void cancelRegistration_RemovesOnlyAPendingAccount() {
        PhoneCode code = service.requestCode(anna);
        service.cancelRegistration(code);
        verify(userRepository).delete(anna);

        User finished = new User();
        finished.setLogin("ben");
        when(userRepository.findByLogin("ben")).thenReturn(finished);
        service.cancelRegistration(new PhoneCode("ben"));
        verify(userRepository, never()).delete(finished);
    }

    @Test
    void removeAbandoned_RemovesWhatIsOlderThanADay() {
        service.removeAbandoned();

        LocalDateTime dayAgo = LocalDateTime.now(clock).minusDays(1);
        verify(userRepository).deletePendingBefore(dayAgo);
        verify(codeRepository).deleteExpiredBefore(dayAgo);
    }
}

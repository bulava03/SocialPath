package com.socialpath;

import com.socialpath.entity.EmailLink;
import com.socialpath.entity.User;
import com.socialpath.repository.EmailLinkRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.EmailVerificationService.Outcome;
import com.socialpath.service.impl.EmailVerificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The email confirmation link: what the letter says, and every way a link
 * stops working.
 */
class EmailVerificationServiceTest {

    private static final String EMAIL = "anna@example.com";
    private static final Instant START = Instant.parse("2026-11-01T10:00:00Z");

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final EmailLinkRepository linkRepository = mock(EmailLinkRepository.class);
    /** The links the repository "stores", by login. */
    private final Map<String, EmailLink> links = new HashMap<>();
    private Clock clock = Clock.fixed(START, ZoneOffset.UTC);
    private User anna;

    @BeforeEach
    void setUp() {
        anna = new User();
        anna.setLogin("anna");
        anna.setFirstName("Anna");
        anna.setEmail(EMAIL);
        when(userRepository.findByLogin("anna")).thenReturn(anna);
        when(linkRepository.save(any(EmailLink.class))).thenAnswer(invocation -> {
            EmailLink link = invocation.getArgument(0);
            links.put(link.getUserLogin(), link);
            return link;
        });
        when(linkRepository.findById(any())).thenAnswer(invocation ->
                Optional.ofNullable(links.get(invocation.<String>getArgument(0))));
        when(linkRepository.findByTokenHash(any())).thenAnswer(invocation -> links.values().stream()
                .filter(link -> link.getTokenHash().equals(invocation.getArgument(0))).findFirst());
    }

    private EmailVerificationServiceImpl service() {
        return new EmailVerificationServiceImpl(linkRepository, userRepository, mailSender, clock,
                "SocialPath <no-reply@socialpath.test>");
    }

    /** Sends a link and returns the token from the letter, the way the user's click would carry it. */
    private String sendAndReadToken() {
        assertTrue(service().sendLink(anna, "http://localhost:8081"));
        ArgumentCaptor<SimpleMailMessage> letter = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, org.mockito.Mockito.atLeastOnce()).send(letter.capture());
        Matcher link = Pattern.compile("http://localhost:8081/email/confirm\\?token=([A-Za-z0-9_-]+)")
                .matcher(letter.getValue().getText());
        assertTrue(link.find(), letter.getValue().getText());
        return link.group(1);
    }

    @Test
    void sendLink_SendsALetterWithALinkAndStoresOnlyItsHash() {
        String token = sendAndReadToken();

        ArgumentCaptor<SimpleMailMessage> letter = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(letter.capture());
        assertEquals(EMAIL, letter.getValue().getTo()[0]);
        assertEquals("SocialPath <no-reply@socialpath.test>", letter.getValue().getFrom());
        EmailLink stored = links.values().iterator().next();
        assertFalse(stored.getTokenHash().contains(token));
        assertEquals(64, stored.getTokenHash().length());
        assertEquals(START.atOffset(ZoneOffset.UTC).toLocalDateTime().plusHours(24), stored.getExpiresAt());
    }

    @Test
    void confirm_TheLinkFromTheLetter_ConfirmsTheAddressOnce() {
        String token = sendAndReadToken();

        assertEquals(Outcome.CONFIRMED, service().confirm(token));
        assertTrue(anna.isEmailVerified());
        verify(userRepository).save(anna);
        verify(linkRepository).delete(any(EmailLink.class));
    }

    @Test
    void confirm_AnythingButTheToken_IsInvalid() {
        sendAndReadToken();

        for (String token : new String[] {null, "", "made-up-token"}) {
            assertEquals(Outcome.INVALID, service().confirm(token), String.valueOf(token));
        }
        assertFalse(anna.isEmailVerified());
    }

    @Test
    void confirm_AfterADay_TheLinkHasExpired() {
        String token = sendAndReadToken();
        clock = Clock.fixed(START.plus(Duration.ofHours(24)).plusSeconds(1), ZoneOffset.UTC);

        assertEquals(Outcome.INVALID, service().confirm(token));
        assertFalse(anna.isEmailVerified());
    }

    @Test
    void confirm_AnOlderLinkAfterANewOneWasSent_IsInvalid() {
        String first = sendAndReadToken();
        String second = sendAndReadToken();

        assertEquals(Outcome.INVALID, service().confirm(first));
        assertEquals(Outcome.CONFIRMED, service().confirm(second));
    }

    @Test
    void confirm_AddressChangedSinceTheLinkWasSent_DoesNotConfirmTheNewOne() {
        String token = sendAndReadToken();
        anna.setEmail("other@example.com");

        assertEquals(Outcome.INVALID, service().confirm(token));
        assertFalse(anna.isEmailVerified());
    }

    @Test
    void confirm_AddressConfirmedByAnotherAccountMeanwhile_IsRefused() {
        String token = sendAndReadToken();
        when(userRepository.isEmailVerifiedByOther(EMAIL, "anna")).thenReturn(true);

        assertEquals(Outcome.EMAIL_TAKEN, service().confirm(token));
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirm_LosingTheRaceForTheUniqueKey_IsRefusedTheSameWay() {
        String token = sendAndReadToken();
        when(userRepository.save(anna)).thenThrow(new DataIntegrityViolationException("uk_users_verified_email"));

        assertEquals(Outcome.EMAIL_TAKEN, service().confirm(token));
    }

    @Test
    void sendLink_MailServerDown_ReportsItInsteadOfFailingThePage() {
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

        assertFalse(service().sendLink(anna, "http://localhost:8081"));
    }

    @Test
    void secondsUntilResend_CountsDownAMinuteFromTheLastLetter() {
        sendAndReadToken();
        EmailLink link = links.values().iterator().next();

        assertEquals(60, service().secondsUntilResend(link));
        clock = Clock.fixed(START.plusSeconds(45), ZoneOffset.UTC);
        assertEquals(15, service().secondsUntilResend(link));
        clock = Clock.fixed(START.plusSeconds(61), ZoneOffset.UTC);
        assertEquals(0, service().secondsUntilResend(link));
    }
}

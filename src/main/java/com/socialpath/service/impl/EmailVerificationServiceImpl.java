package com.socialpath.service.impl;

import com.socialpath.entity.EmailLink;
import com.socialpath.entity.User;
import com.socialpath.repository.EmailLinkRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.EmailVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    /** The path the link in the letter opens. */
    public static final String CONFIRM_PATH = "/email/confirm";

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final Clock clock;
    private final String from;

    public EmailVerificationServiceImpl(EmailLinkRepository linkRepository, UserRepository userRepository,
                                        JavaMailSender mailSender, Clock clock,
                                        @Value("${app.mail.from}") String from) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
        this.clock = clock;
        this.from = from;
    }

    @Override
    @Transactional
    public boolean sendLink(User user, String baseUrl) {
        // 32 random bytes: the link cannot be guessed, and its hash is all the database holds.
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        LocalDateTime now = now();
        // One row per user, overwritten: the older link stops working.
        EmailLink link = linkRepository.findById(user.getLogin()).orElseGet(EmailLink::new);
        link.setUserLogin(user.getLogin());
        link.setTokenHash(hash(token));
        link.setEmail(user.getEmail());
        link.setSentAt(now);
        link.setExpiresAt(now.plus(LINK_LIFETIME));
        linkRepository.save(link);

        String url = UriComponentsBuilder.fromUriString(baseUrl).path(CONFIRM_PATH)
                .queryParam("token", token).toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(user.getEmail());
        message.setSubject("Confirm your email for SocialPath");
        message.setText("Hi " + user.getFirstName() + ",\n\n"
                + "open this link to confirm that " + user.getEmail() + " is your address on SocialPath:\n\n"
                + url + "\n\n"
                + "The link works for " + LINK_LIFETIME.toHours() + " hours. "
                + "If you did not ask for it, ignore this letter: nothing changes until the link is opened.\n");
        try {
            mailSender.send(message);
            return true;
        } catch (MailException e) {
            log.warn("Could not send the confirmation letter to {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<EmailLink> findFor(String login) {
        return linkRepository.findById(login);
    }

    @Override
    public Outcome confirm(String token) {
        if (token == null || token.isBlank()) {
            return Outcome.INVALID;
        }
        Optional<EmailLink> found = linkRepository.findByTokenHash(hash(token));
        if (found.isEmpty() || !found.get().getExpiresAt().isAfter(now())) {
            return Outcome.INVALID;
        }
        EmailLink link = found.get();
        User user = userRepository.findByLogin(link.getUserLogin());
        // A link confirms the address it went to, not one the profile has changed to since.
        if (user == null || !link.getEmail().equalsIgnoreCase(user.getEmail())) {
            return Outcome.INVALID;
        }
        if (userRepository.isEmailVerifiedByOther(user.getEmail(), user.getLogin())) {
            return Outcome.EMAIL_TAKEN;
        }

        user.setEmailVerified(true);
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two accounts confirming one address at the same moment: the unique key lets one through.
            return Outcome.EMAIL_TAKEN;
        }
        linkRepository.delete(link);
        return Outcome.CONFIRMED;
    }

    @Override
    public long secondsUntilResend(EmailLink link) {
        long seconds = Duration.between(now(), link.getSentAt().plus(RESEND_AFTER)).toSeconds();
        return Math.max(0, seconds);
    }

    @Override
    public void forget(String login) {
        linkRepository.deleteById(login);
    }

    @Override
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT3M")
    public void removeExpired() {
        linkRepository.deleteExpiredBefore(now());
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }
}

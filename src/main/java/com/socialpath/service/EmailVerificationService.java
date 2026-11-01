package com.socialpath.service;

import com.socialpath.entity.EmailLink;
import com.socialpath.entity.User;

import java.time.Duration;
import java.util.Optional;

/**
 * Confirms that a user owns the email address in their profile: a letter
 * with a one-time link goes to the address, and opening the link confirms
 * it. A confirmed address signs in like the login and can be shown to others.
 */
public interface EmailVerificationService {

    /** How long a link works. */
    Duration LINK_LIFETIME = Duration.ofHours(24);
    /** How soon another letter can be sent. */
    Duration RESEND_AFTER = Duration.ofSeconds(60);

    /** What opening a link led to. */
    enum Outcome {
        /** The address is confirmed now. */
        CONFIRMED,
        /** No such link: never sent, used already, replaced by a newer one, or past its time. */
        INVALID,
        /** Another account confirmed the same address first. */
        EMAIL_TAKEN
    }

    /**
     * Sends a confirmation link to the user's address, replacing any earlier one.
     * @param user the user, with the address to confirm
     * @param baseUrl where the app is reached, e.g. "http://localhost:8080", for the link
     * @return false when the letter could not be handed to the mail server
     */
    boolean sendLink(User user, String baseUrl);

    /**
     * The link last sent to a user.
     * @param login the user's login
     * @return the link, if one was sent and not used yet
     */
    Optional<EmailLink> findFor(String login);

    /**
     * Confirms the address a link was sent to.
     * @param token the token from the link
     * @return the outcome
     */
    Outcome confirm(String token);

    /**
     * How long until another letter can be sent.
     * @param link the link last sent
     * @return whole seconds, 0 when a new one can go now
     */
    long secondsUntilResend(EmailLink link);

    /**
     * Drops a waiting link, e.g. after the address changed.
     * @param login the user's login
     */
    void forget(String login);

    /**
     * Removes links that can no longer be used.
     */
    void removeExpired();
}

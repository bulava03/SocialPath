package com.socialpath.service;

import com.socialpath.entity.PhoneCode;
import com.socialpath.entity.User;

import java.time.Duration;
import java.util.Optional;

/**
 * Confirms that a user has the phone they entered: a six-digit code goes to
 * the number by SMS and has to be typed back. Confirming finishes a pending
 * registration and lets the number be used to sign in.
 */
public interface PhoneVerificationService {

    /** How long a code can be used. */
    Duration CODE_LIFETIME = Duration.ofMinutes(10);
    /** Wrong codes allowed before a new one has to be sent. */
    int MAX_ATTEMPTS = 5;
    /** How soon another code can be sent. */
    Duration RESEND_AFTER = Duration.ofSeconds(60);
    /** How long a registration may wait for its code before it is removed. */
    Duration PENDING_LIFETIME = Duration.ofDays(1);

    /** What entering a code led to. */
    enum Outcome {
        /** The number is confirmed now. */
        CONFIRMED,
        /** Not the code that was sent; it can be tried again. */
        WRONG_CODE,
        /** The code has run out (time, attempts, or the number changed); a new one is needed. */
        EXPIRED,
        /** Another account confirmed the same number first. */
        NUMBER_TAKEN
    }

    /**
     * Sends a code to the user's number. A code sent less than
     * {@link #RESEND_AFTER} ago to the same number is kept instead, so
     * reloading or signing in again does not flood the phone.
     * @param user a user with a phone number in E.164
     * @return the code now waiting to be entered
     */
    PhoneCode requestCode(User user);

    /**
     * A code by the id in the confirmation page's address.
     * @param id the code id
     * @return the code, if it exists
     */
    Optional<PhoneCode> find(String id);

    /**
     * The code last sent to a user.
     * @param login the user's login
     * @return the code, if one was sent and not used yet
     */
    Optional<PhoneCode> findFor(String login);

    /**
     * Checks a typed code and, when it is right, confirms the number.
     * @param code the code that was sent
     * @param typed what the user entered
     * @return the outcome
     */
    Outcome confirm(PhoneCode code, String typed);

    /**
     * How long until another code can be sent.
     * @param code the code last sent
     * @return whole seconds, 0 when a new one can go now
     */
    long secondsUntilResend(PhoneCode code);

    /**
     * Removes a registration that still waits for its code, with the code.
     * A finished account is left alone.
     * @param code the registration's code
     */
    void cancelRegistration(PhoneCode code);

    /**
     * Removes registrations that were never confirmed, and old codes.
     */
    void removeAbandoned();
}

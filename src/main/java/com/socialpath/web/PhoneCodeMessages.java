package com.socialpath.web;

import com.socialpath.entity.PhoneCode;
import com.socialpath.service.PhoneVerificationService;
import com.socialpath.service.PhoneVerificationService.Outcome;
import com.socialpath.service.UserService;

/**
 * What the code forms (registration and the Security page) say when a code
 * is not accepted.
 */
final class PhoneCodeMessages {

    private PhoneCodeMessages() {
    }

    /**
     * The message for an outcome other than {@link Outcome#CONFIRMED}.
     * @param outcome what entering the code led to
     * @param code the code, with the wrong attempts counted
     * @return the text for the form
     */
    static String of(Outcome outcome, PhoneCode code) {
        return switch (outcome) {
            case WRONG_CODE -> {
                int left = PhoneVerificationService.MAX_ATTEMPTS - code.getAttempts();
                yield "Wrong code. " + left + (left == 1 ? " attempt" : " attempts") + " left.";
            }
            case EXPIRED -> "This code can no longer be used. Send a new one.";
            case NUMBER_TAKEN -> UserService.PHONE_TAKEN;
            case CONFIRMED -> "";
        };
    }
}

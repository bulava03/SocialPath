package com.socialpath.exception;

/**
 * Thrown when the authenticated account is banned. Kept as an explicit signal
 * so the ban message can be surfaced consistently instead of being re-checked
 * inside every controller method.
 */
public class UserBannedException extends RuntimeException {
    public UserBannedException(String message) {
        super(message);
    }
}

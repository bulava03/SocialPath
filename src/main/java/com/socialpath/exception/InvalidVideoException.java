package com.socialpath.exception;

/**
 * An uploaded video that cannot be accepted: unreadable, or over a limit such
 * as the maximum length. The message is meant to be shown to the user.
 */
public class InvalidVideoException extends IllegalArgumentException {
    public InvalidVideoException(String message) {
        super(message);
    }
}

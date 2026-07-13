package com.socialpath.exception;

/**
 * Thrown when an authenticated user attempts an action they have no right to
 * perform (deleting someone else's publication, managing a group they do not
 * own, and so on). Translated to a redirect for page controllers and to the
 * error code for REST controllers by the exception handlers.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}

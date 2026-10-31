package com.socialpath.security;

/**
 * The id of the session the current request was authenticated with. The JWT
 * filter stores it as a request attribute; controllers read it with
 * {@code @RequestAttribute(CurrentSession.ATTRIBUTE)}.
 */
public final class CurrentSession {

    /** The request attribute name. */
    public static final String ATTRIBUTE = "socialpath.sessionId";

    private CurrentSession() {
    }
}

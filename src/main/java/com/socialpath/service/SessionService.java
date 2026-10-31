package com.socialpath.service;

import com.socialpath.dto.response.SessionView;
import com.socialpath.entity.UserSession;

import java.util.List;
import java.util.Optional;

/**
 * Signed-in browsers. A session token is accepted only while its session
 * exists and has not expired; using it moves the expiry forward.
 */
public interface SessionService {

    /**
     * Opens a session for a successful sign-in.
     * @param login who signed in
     * @param ip where the request came from
     * @param userAgent the browser's User-Agent header, may be null
     * @return the new session
     */
    UserSession open(String login, String ip, String userAgent);

    /**
     * The session a token names, if it can still be used.
     * @param id the session id from the token
     * @param login the login from the same token
     * @return the session when it exists, belongs to that login and has not expired
     */
    Optional<UserSession> findActive(String id, String login);

    /**
     * Records a request made with the session. To keep writes rare this only
     * happens once per refresh interval: then the last-seen time and the
     * address are updated and the expiry moves forward.
     * @param session the session the request was made with
     * @param ip where the request came from
     * @return true when the expiry moved, so the browser needs a fresh token
     */
    boolean touch(UserSession session, String ip);

    /**
     * The user's sessions: the current one first, then the most recently used.
     * @param login whose sessions
     * @param currentId the session the list is viewed in, marked as current
     * @return the sessions
     */
    List<SessionView> list(String login, String currentId);

    /**
     * Ends a session; does nothing when it does not exist (anymore).
     * @param id the session, may be null
     */
    void end(String id);

    /**
     * Ends one of the user's sessions; a session of another user is left alone.
     * @param login whose session it must be
     * @param id the session
     */
    void end(String login, String id);

    /**
     * Ends every session of the user except the one given.
     * @param login whose sessions
     * @param keepId the session to keep
     */
    void endOthers(String login, String keepId);

    /** Deletes sessions past their expiry. */
    void removeExpired();
}

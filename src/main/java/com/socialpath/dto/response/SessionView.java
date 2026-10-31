package com.socialpath.dto.response;

import com.socialpath.entity.UserSession;
import com.socialpath.session.DeviceInfo;

import java.time.LocalDateTime;

/**
 * A session as the Security page lists it.
 * @param id the session id, for the "End" button
 * @param browser e.g. "Chrome"
 * @param os e.g. "Windows"
 * @param ip the address of the last request
 * @param location where that address is, or null when unknown
 * @param createdAt when the user signed in
 * @param lastSeenAt the last request, to the refresh interval
 * @param current whether this is the session the page is viewed in
 */
public record SessionView(String id, String browser, String os, String ip, String location,
                          LocalDateTime createdAt, LocalDateTime lastSeenAt, boolean current) {

    public static SessionView from(UserSession session, String currentId) {
        DeviceInfo device = DeviceInfo.from(session.getUserAgent());
        return new SessionView(session.getId(), device.browser(), device.os(), session.getIp(),
                session.getLocation(), session.getCreatedAt(), session.getLastSeenAt(),
                session.getId().equals(currentId));
    }
}

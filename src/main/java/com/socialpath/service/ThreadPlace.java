package com.socialpath.service;

/**
 * Where a thread lives, as stored on the post at its top.
 * @param groupId the group, or null for a thread on a user page
 * @param pageOwner the user whose page it is (the post's author); for a
 *                  group thread, simply the author of the post
 */
public record ThreadPlace(Long groupId, String pageOwner) {

    public boolean inGroup() {
        return groupId != null;
    }
}

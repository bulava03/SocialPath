package com.socialpath.repository;

/** How many direct replies a post or comment has. */
public interface ReplyCount {
    Long getParentId();

    Long getTotal();
}

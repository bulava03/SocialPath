package com.socialpath.dto.response;

/**
 * The newest post or comment in a feed written by someone other than the
 * viewer; an open feed compares it with what it has shown.
 * @param latest the largest such publication id, 0 when there is none
 */
public record LatestActivity(long latest) {
}

package com.socialpath.dto.response;

import java.util.List;

/**
 * One portion of a feed, newest posts first.
 * @param posts the posts of this portion with their comment trees
 * @param nextBefore id to continue from (the oldest post here), or null when
 *                   there is nothing older
 */
public record FeedPage(List<PublicationPresentable> posts, Long nextBefore) {
}

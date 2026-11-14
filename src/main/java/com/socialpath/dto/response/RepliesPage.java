package com.socialpath.dto.response;

import java.util.List;

/**
 * One portion of the direct replies to a post or comment, newest first.
 * @param replies the replies of this portion; their own replies are not
 *                loaded, only counted
 * @param nextBefore id to continue from (the oldest reply here), or null
 *                   when there is nothing older
 * @param hidden how many older replies are left after this portion
 * @param next how many of them the next portion brings
 */
public record RepliesPage(List<PublicationPresentable> replies, Long nextBefore, long hidden, long next) {
}

package com.socialpath.dto.response;

import java.util.List;

/**
 * Combined result of a site-wide search: users and groups matched separately.
 * @param users matched users
 * @param groups matched groups
 */
public record SearchResults(List<UserSearchResult> users, List<GroupSearchResult> groups) {
}

package com.socialpath.service;

import com.socialpath.dto.response.SearchResults;

import java.io.IOException;

/**
 * Site-wide search across users and groups.
 */
public interface SearchService {

    /**
     * Searches users and groups matching the given text.
     * @param searchValuesString the raw search text
     * @param thisLogin the searching user's login (to contextualize results)
     * @return matched users and groups
     */
    SearchResults searchUsersAndGroups(String searchValuesString, String thisLogin) throws IOException;
}

package com.example.SocialPath.service;

import java.io.IOException;

public interface SearchService {
    Object[] searchUsersAndGroups(String searchValuesString, String thisLogin) throws IOException;
}

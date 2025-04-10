package com.example.SocialPath.service;

import com.example.SocialPath.extraClasses.UserSearchResult;

import java.util.List;

public interface GeoSearchService {
    List<UserSearchResult> findNearest(double userLat, double userLon, List<UserSearchResult> locations, int limit);
}

package com.example.SocialPath.service.impl;

import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.service.GeoSearchService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeoSearchServiceImpl implements GeoSearchService {
    private static final double EARTH_RADIUS = 6371.0; // Радіус Землі в км

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public static List<UserSearchResult> mergeAlternating(
            List<UserSearchResult> remoteServices,
            List<UserSearchResult> localServices
    ) {
        List<UserSearchResult> result = new ArrayList<>();
        int maxSize = Math.max(remoteServices.size(), localServices.size());

        for (int i = 0; i < maxSize; i++) {
            if (i < remoteServices.size()) {
                result.add(remoteServices.get(i));
            }
            if (i < localServices.size()) {
                result.add(localServices.get(i));
            }
        }

        return result;
    }


    @Override
    public List<UserSearchResult> findNearest(double userLat, double userLon, List<UserSearchResult> locations, int limit) {
        if (limit < 6) {
            limit = 6;
        }

        List<UserSearchResult> remoteServices = locations.stream()
                .filter(UserSearchResult::isOnlyOnline)
                .limit(limit / 2)
                .toList();

        int remainingLimit = limit - remoteServices.size();

        List<UserSearchResult> localServices = locations.stream()
                .filter(loc -> !loc.isOnlyOnline())
                .sorted(Comparator.comparingDouble(loc ->
                        haversine(userLat, userLon, loc.getLatitude(), loc.getLongitude())))
                .limit(remainingLimit)
                .toList();

        if (localServices.size() < remainingLimit) {
            remainingLimit = limit / 2 + (remainingLimit - localServices.size());
            remoteServices = locations.stream()
                    .filter(UserSearchResult::isOnlyOnline)
                    .limit(remainingLimit)
                    .toList();
        }

        return mergeAlternating(remoteServices, localServices);
    }
}

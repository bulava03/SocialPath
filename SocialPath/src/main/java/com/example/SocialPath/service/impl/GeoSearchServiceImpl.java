package com.example.SocialPath.service.impl;

import com.example.SocialPath.extraClasses.UserSearchResult;
import com.example.SocialPath.service.GeoSearchService;
import com.example.SocialPath.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeoSearchServiceImpl implements GeoSearchService {
    @Autowired
    private GradeService gradeService;

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

    private List<UserSearchResult> interleaveProportionally(List<UserSearchResult> offline, List<UserSearchResult> online) {
        List<UserSearchResult> result = new ArrayList<>();
        int oSize = offline.size();
        int lSize = online.size();

        if (oSize == 0) return online;
        if (lSize == 0) return offline;

        double ratio = (double) oSize / lSize;
        int i = 0, j = 0;
        double counter = 0;

        while (i < oSize || j < lSize) {
            if ((j >= lSize) || (i < oSize && counter < ratio)) {
                result.add(offline.get(i++));
                counter += 1;
            } else {
                result.add(online.get(j++));
                counter -= ratio;
            }
        }

        return result;
    }

    @Override
    public List<UserSearchResult> findNearest(double userLat, double userLon, List<UserSearchResult> locations) {
        double alpha = 0.2;  // вага відстані — менша, бо це менш важливо
        double beta = 0.2;   // вага рейтингу — теж менш важлива
        double gamma = 0.6;  // вага співпадінь — основна

        // Максимальна відстань для нормалізації
        double maxDistance = locations.stream()
                .filter(loc -> !loc.isOnlyOnline())
                .mapToDouble(loc -> haversine(userLat, userLon, loc.getLatitude(), loc.getLongitude()))
                .max()
                .orElse(1.0); // захист від ділення на 0

        // Максимальна кількість співпадінь для нормалізації
        int maxMatches = locations.stream()
                .mapToInt(UserSearchResult::getMatches)  // припускаємо, що є метод getMatches()
                .max()
                .orElse(1);

        for (UserSearchResult loc : locations) {
            double r = gradeService.getAverageGrade(loc.getLogin()); // 1–5 або 0, якщо не задано
            double R = (r > 0) ? (r - 1.0) / 4.0 : 0.5;

            int matches = loc.getMatches();
            double matchesNormalized = (double) matches / maxMatches;

            if (loc.isOnlyOnline()) {
                loc.setScore(gamma * matchesNormalized + beta * R);
            } else {
                double d = haversine(userLat, userLon, loc.getLatitude(), loc.getLongitude());
                double D = 1.0 - d / maxDistance;
                loc.setScore(alpha * D + beta * R + gamma * matchesNormalized);
            }
        }

        List<UserSearchResult> online = locations.stream()
                .filter(UserSearchResult::isOnlyOnline)
                .sorted(Comparator.comparingDouble(UserSearchResult::getScore).reversed())
                .collect(Collectors.toList());

        List<UserSearchResult> offline = locations.stream()
                .filter(loc -> !loc.isOnlyOnline())
                .sorted(Comparator.comparingDouble(UserSearchResult::getScore).reversed())
                .collect(Collectors.toList());

        return interleaveProportionally(offline, online);
    }

}

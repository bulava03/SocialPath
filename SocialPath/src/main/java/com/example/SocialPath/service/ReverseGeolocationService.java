package com.example.SocialPath.service;

import java.util.List;

public interface ReverseGeolocationService {
    List<String> normalizeQuery(String query);
    String getAddressFromCoordinates(double lat, double lon);
    int countMatches(List<String> queryWords, String concreteAddress);
}

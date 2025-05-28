package com.example.SocialPath.service.impl;

import com.example.SocialPath.service.ReverseGeolocationService;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

@Service
public class ReverseGeolocationServiceImpl implements ReverseGeolocationService {

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public List<String> normalizeQuery(String query) {
        return Arrays.stream(query.toLowerCase().replaceAll("[,;]", "").split("\\s+"))
                .filter(word -> !word.isBlank())
                .toList();
    }

    @Override
    public String getAddressFromCoordinates(double lat, double lon) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat="
                    + lat + "&lon=" + lon;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "geo-search-app")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject obj = new JSONObject(response.body());

            return obj.optString("display_name", "");
        } catch (Exception e) {
            System.err.println("Error in geocoding: " + e.getMessage());
            return "";
        }
    }

    @Override
    public int countMatches(List<String> queryWords, String concreteAddress) {
        if (concreteAddress == null || concreteAddress.isEmpty()) {
            return 0;
        }

        List<String> normalizedQueryWords = queryWords.stream()
                .map(String::toLowerCase)
                .toList();

        List<String> addressWords = normalizeQuery(concreteAddress);

        int matches = 0;
        for (String word : normalizedQueryWords) {
            if (addressWords.contains(word)) {
                matches++;
            }
        }
        return matches;
    }
}

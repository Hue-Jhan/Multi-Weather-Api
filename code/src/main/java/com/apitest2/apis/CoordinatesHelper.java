package com.apitest2.apis;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;


public class CoordinatesHelper {

    @Value("${app.api.coord}")
    private static String API_KEY;
    
    public static double[] getCoordinates(String city) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String query = URLEncoder.encode(city, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://geocode.maps.co/search?q=" + query + "&api_key=" + API_KEY)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode arr = mapper.readTree(response.body());
        if (arr.isEmpty()) return null;
        double lat = arr.get(0).get("lat").asDouble();
        double lon = arr.get(0).get("lon").asDouble();
        // saveCoordinates(city, lat, lon);
        // System.out.println(lat + " " + lon);
        return new double[]{lat, lon};
    }
}

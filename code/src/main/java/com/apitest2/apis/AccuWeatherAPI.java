package com.apitest2.apis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.apitest2.datixd.CurrentWeatherData;
import com.apitest2.datixd.DailyWeatherData;
import com.apitest2.datixd.HourlyWeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


// accuweather sucks dont use it
@Service
public class AccuWeatherAPI implements WeatherApi {

    @Value("${app.api.accuweth2}")
    private String apikey;

    public String regionData = "";
    // private String source = "AccuWeatherAPI";

    @Override
    public void getAllWeather(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return;
        }

        System.out.println("inutil");
        // getCurrentConditions(locationKey);
        // getDailyForecastSium(locationKey);
        // getFiveDayForecast(locationKey);
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return null;
        }

        String url = "http://dataservice.accuweather.com/currentconditions/v1/" + locationKey
            + "?apikey=" + apikey + "&language=en&details=true";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            if (root.isEmpty() || !root.isArray()) {
                System.out.println("No data found.");
                return null;
            }

            JsonNode obj = root.get(0);
            double temp = obj.get("Temperature").get("Metric").get("Value").asDouble();
            String weatherText = obj.get("WeatherText").asText();
            if (weatherText.toLowerCase().contains("clear")) {
                weatherText = "Sunny";
            }
            double windSpeed = obj.get("Wind").get("Speed").get("Metric").get("Value").asDouble();
            int windDir = obj.get("Wind").get("Direction").get("Degrees").asInt();

            return new CurrentWeatherData(temp, windSpeed, "AccuWeather", weatherText, windDir);
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        String locationKey = getLocationKey(city);
        if (locationKey == null) {
            System.out.println("City not found.");
            return null;
        }

        String url = "http://dataservice.accuweather.com/forecasts/v1/daily/5day/" + locationKey
                + "?apikey=" + apikey + "&language=en&details=true&metric=true";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        if (root.isEmpty()) {
            System.out.println("No forecast data found.");
            return null;
        }

        JsonNode dailyForecasts = root.get("DailyForecasts");
        if (dailyForecasts == null || !dailyForecasts.isArray()) {
            System.out.println("Invalid forecast response.");
            return null;
        }

        List<DailyWeatherData> result = new ArrayList<>();
        for (JsonNode day : dailyForecasts) {
            String date = day.get("Date").asText().split("T")[0];

            JsonNode temp = day.get("Temperature");
            double min = temp.get("Minimum").get("Value").asDouble();
            double max = temp.get("Maximum").get("Value").asDouble();

            String dayPhrase = day.get("Day").get("IconPhrase").asText();
            double dayWind = day.get("Day").get("Wind").get("Speed").get("Value").asDouble();
            int windDir = day.get("Day").get("Wind").get("Direction").get("Degrees").asInt();

            DailyWeatherData dwd = new DailyWeatherData(
                    date, max, min, dayWind, windDir, dayPhrase, "AccuWeather"
            );
            result.add(dwd);
        }

        if (!result.isEmpty()) {
            List<HourlyWeatherData> hourlyData = getHourlyData(locationKey);
            if (hourlyData != null && !hourlyData.isEmpty()) {
                result.get(0).hourlyData.addAll(hourlyData);
            }
        }

        return result;
    }

    private List<HourlyWeatherData> getHourlyData(String locationKey) throws Exception {
        String url = "http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/"
                + locationKey + "?apikey=" + apikey + "&details=true&metric=true";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        if (root == null || !root.isArray() || root.isEmpty()) {
            System.out.println("No hourly forecast data found.");
            return Collections.emptyList();
        }

        List<HourlyWeatherData> hourlyList = new ArrayList<>();
        for (JsonNode h : root) {
            String dateTime = h.get("DateTime").asText();
            double temp = h.get("Temperature").get("Value").asDouble();
            double windSpeed = h.get("Wind").get("Speed").get("Value").asDouble();
            int windDir = h.get("Wind").get("Direction").get("Degrees").asInt();
            String phrase = h.get("IconPhrase").asText();

            HourlyWeatherData hour = new HourlyWeatherData(
                    dateTime, temp, windSpeed, windDir, phrase, "AccuWeather"
            );
            hourlyList.add(hour);
        }

        return hourlyList;
    }



    private String getLocationKey(String city) throws Exception {
        String url = "http://dataservice.accuweather.com/locations/v1/cities/IT/search"
                + "?apikey=" + apikey + "&q=" + city;


        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        if (root.isEmpty() || !root.isArray()) {
            System.out.println("No data found rippp");
            return null;
        }

        JsonNode firstResult = root.get(0);
        String locationKey = firstResult.get("Key").asText();
        String cityName = firstResult.get("LocalizedName").asText();
        String countryName = firstResult.get("Country").get("LocalizedName").asText();
        String regionName = firstResult.get("AdministrativeArea").get("LocalizedName").asText();
        this.regionData = cityName + ", " + regionName + ", " + countryName;

        System.out.println("Weather forrrr " + cityName + ", " + regionName + ", " + countryName + ":");
        return locationKey;
    }

}

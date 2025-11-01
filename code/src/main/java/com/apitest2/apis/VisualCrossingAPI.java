package com.apitest2.apis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.apitest2.datixd.CurrentWeatherData;
import com.apitest2.datixd.DailyWeatherData;
import com.apitest2.datixd.HourlyWeatherData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class VisualCrossingAPI implements WeatherApi {

    @Value("${app.api.visualcro}")
    private String visualcroApikey;

    @Override
    public void getAllWeather(String city) throws Exception {
        String baseUrl = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/";
        String url = baseUrl + city + "?unitGroup=us&include=days%2Ccurrent&key=" + visualcroApikey + "&contentType=json";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        System.out.println("\nCity: " + root.get("resolvedAddress").asText());
        System.out.println();

        JsonNode days = root.get("days");
        if (days == null || !days.isArray()) {
            System.out.println("No forecast data found.");
            return;
        }

        for (JsonNode day : days) {
            String date = day.get("datetime").asText();
            double tempMaxF = day.get("tempmax").asDouble();
            double tempMinF = day.get("tempmin").asDouble();
            double windSpeedMph = day.get("windspeed").asDouble();
            double windGustMph = day.has("windgust") ? day.get("windgust").asDouble() : 0;
            double windDir = day.get("winddir").asDouble();
            String windDirCompass = windDirectionToCompass(windDir);
            String conditions = day.get("conditions").asText();
            String sunrise = day.get("sunrise").asText();
            String sunset = day.get("sunset").asText();
            double tempMaxC = Math.ceil(((tempMaxF - 32) * 5 / 9) * 10) / 10.0;
            double tempMinC = Math.ceil(((tempMinF - 32) * 5 / 9) * 10) / 10.0;
            double windSpeedKmh = windSpeedMph * 1.60934;
            double windGustKmh = windGustMph * 1.60934;

            System.out.printf(
                    "%s -> Max/Min T: %.0f/%.0f°C | Wind: %.1f/%.1f km/h %s | %s | Sunrise: %s | Sunset: %s%n",
                    date, tempMaxC, tempMinC, windSpeedKmh, windGustKmh, windDirCompass, conditions, sunrise, sunset
            );
        }
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        // Dotenv dotenv = Dotenv.load(); // Loads the .env file automatically
        // visualcroApikey = dotenv.get("APP_API_VISUALCRO");
        double[] coordinates = CoordinatesHelper.getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not foundnnn.");
            return null;
        }
        double lat = coordinates[0];
        double lon = coordinates[1];
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
           + lat + "," + lon + "?unitGroup=metric&include=current&key=" + visualcroApikey + "&contentType=json";

        // System.out.println("Requesting URL: " + url);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            String body = response.body();
            if (response.statusCode() != 200 || !body.trim().startsWith("{")) {
                System.out.println("Invalid response from API:");
                // System.out.println(body);
                // System.out.println("HTTP Status: " + response.statusCode());
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            // System.out.println(response.body());

            JsonNode current = root.get("currentConditions");
            if (current == null) {
                if (root.has("days") && root.get("days").size() > 0) {
                    System.out.println("no current, getting today's avg");
                    current = root.get("days").get(0);
                } else {
                    System.out.println("No weather data available");
                    return null;
                }
            }

            double temp = current.get("temp").asDouble();
            double windSpeedMph = current.get("windspeed").asDouble();
            int windDir = current.get("winddir").asInt();
            String conditions = current.get("conditions").asText();

            double windSpeedKmh = Math.round(windSpeedMph * 1.60934 * 10.0) / 10.0;
            if (conditions.toLowerCase().contains("clear")) conditions = "Sunny";

            return new CurrentWeatherData(temp, windSpeedKmh, "VisualCrossing", conditions, windDir);
        } catch (Exception e) {
            System.out.println(e);
        }
    return null;
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
       double[] coordinates = CoordinatesHelper.getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return null;
        }

        double lat = coordinates[0];
        double lon = coordinates[1];
        String url = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/"
                + lat + "," + lon
                + "?unitGroup=metric&include=days%2Chours&key=" + visualcroApikey + "&contentType=json";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());
        JsonNode days = root.get("days");
        String timezone = root.get("timezone").asText();
        List<DailyWeatherData> result = new ArrayList<>();

        for (JsonNode day : days) {
            String date = day.get("datetime").asText();
            double tempMax = day.get("tempmax").asDouble();
            double tempMin = day.get("tempmin").asDouble();
            double windSpeed = Math.round(day.get("windspeed").asDouble() * 10.0) / 10.0;
            int windDir = day.get("winddir").asInt();
            String conditions = day.get("conditions").asText();
            if (conditions.toLowerCase().contains("clear")) conditions = "Sunny";

            DailyWeatherData dwd = new DailyWeatherData(
                    date, tempMax, tempMin, windSpeed, windDir, conditions, "VisualCrossing"
            );

            JsonNode hours = day.get("hours");
            if (hours != null && hours.isArray()) {
                List<HourlyWeatherData> hourlyList = new ArrayList<>();
                for (JsonNode hour : hours) {
                    String dayDate = day.get("datetime").asText();
                    String timePart = hour.get("datetime").asText();

                    LocalDate datePart = LocalDate.parse(dayDate);
                    LocalTime time = LocalTime.parse(timePart);
                    ZonedDateTime zoned = LocalDateTime.of(datePart, time)
                            .atZone(ZoneId.of(timezone));
                    String isoDatetime = zoned.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

                    double temp = hour.get("temp").asDouble();
                    double windSpeedHour = Math.round(hour.get("windspeed").asDouble() * 10.0) / 10.0;
                    int windDirHour = hour.get("winddir").asInt();
                    String hourConditions = hour.get("conditions").asText();
                    if (hourConditions.toLowerCase().contains("clear")) hourConditions = "Sunny";

                    hourlyList.add(new HourlyWeatherData(
                            isoDatetime, temp, windSpeedHour, windDirHour, hourConditions, "VisualCrossing"
                    ));
                }
                dwd.hourlyData.addAll(hourlyList);
            }
            result.add(dwd);
        }
        return result;
    }

    public static String windDirectionToCompass(double degrees) {
        String[] directions = {
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };
        int index = (int) Math.round(((degrees % 360) / 22.5));
        return directions[index % 16];
    }

}

package com.apitest2.apis;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
public class OpenMeteoAPI implements WeatherApi {


    @Value("${app.api.openweath}")
    @SuppressWarnings("unused")
    private String apikey;

    @Override
    public void getAllWeather(String city) throws Exception {
        System.out.println("inutil");
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        double[] coordinates = CoordinatesHelper.getCoordinates(city);
        if (coordinates == null) {
            System.out.println("City not found.");
            return null;
        }
        double lat = coordinates[0];
        double lon = coordinates[1];

        String url = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=" + lat
            + "&longitude=" + lon
            + "&timezone=Europe%2FBerlin"
            + "&forecast_days=3"
            + "&current=temperature_2m,weather_code,precipitation,rain,wind_speed_10m,wind_direction_10m";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            JsonNode current = root.get("current");
            if (current == null || current.isEmpty()) {
                System.out.println("No current weather data found.");
                return null;
            }

            double temp = current.get("temperature_2m").asDouble();
            double wind = current.get("wind_speed_10m").asDouble();
            int weatherCode = current.get("weather_code").asInt();
            int winddir = current.get("wind_direction_10m").asInt();

            return new CurrentWeatherData(temp, wind, "OpenMeteo", getWeatherDescription(weatherCode), winddir);
            
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    private String getWeatherDescription(int code) {
        boolean isNight = false;
        if (code >= 100) {
            isNight = true;
            code -= 100;
        }
        String description;
        if (code >= 0 && code <= 3) {
            description = "Sunny";
        } else if (code >= 4 && code <= 5) {
            description = "Partly Cloudy";
        } else if (code >= 6 && code <= 7) {
            description = "Cloudy";
        } else if (code == 10) {
            description = "Mist";
        } else if (code == 11) {
            description = "Fog";
        } else if (code >= 20 && code <= 29) {
            description = "Precipitation";
        } else if (code >= 30 && code <= 35) {
            description = "Dust or Sand";
        } else if (code >= 40 && code <= 49) {
            description = "Fog or Ice Fog";
        } else if (code >= 50 && code <= 55) {
            description = "Drizzle";
        } else if (code >= 56 && code <= 57) {
            description = "Freezing Drizzle";
        } else if (code >= 60 && code <= 65) {
            description = "Rain";
        } else if (code >= 66 && code <= 67) {
            description = "Freezing Rain";
        } else if (code >= 68 && code <= 69) {
            description = "Rain and Snow";
        } else if (code >= 70 && code <= 79) {
            description = "Snow";
        } else if (code >= 80 && code <= 82) {
            description = "Showers";
        } else if (code >= 90 && code <= 95) {
            description = "Thunderstorm";
        } else if (code >= 96 && code <= 99) {
            description = "Thunderstorm with Hail";
        } else {
            description = "Unknown";
        }
        if (isNight) {
            description += " Night";
        }
        return description;
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

        String url = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=" + lat
            + "&longitude=" + lon
            + "&daily=temperature_2m_max,temperature_2m_min,weather_code,rain_sum,wind_speed_10m_max,wind_direction_10m_dominant"
            + "&hourly=temperature_2m,weather_code,wind_speed_10m,wind_direction_10m"
            + "&timezone=Europe%2FBerlin"
            + "&past_days=1"
            + "&forecast_days=4";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        JsonNode daily = root.get("daily");
        JsonNode hourly = root.get("hourly");
        if (daily == null || hourly == null) {
            System.out.println("No forecast data found.");
            return null;
        }

        JsonNode dates = daily.get("time");
        JsonNode tempsMax = daily.get("temperature_2m_max");
        JsonNode tempsMin = daily.get("temperature_2m_min");
        JsonNode weatherCodes = daily.get("weather_code");
        JsonNode windSpeeds = daily.get("wind_speed_10m_max");
        JsonNode windDirs = daily.get("wind_direction_10m_dominant");

        JsonNode timesHourly = hourly.get("time");
        JsonNode tempsHourly = hourly.get("temperature_2m");
        JsonNode weatherCodesHourly = hourly.get("weather_code");
        JsonNode windSpeedsHourly = hourly.get("wind_speed_10m");
        JsonNode windDirsHourly = hourly.get("wind_direction_10m");

        List<DailyWeatherData> result = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            String date = dates.get(i).asText();
            double tempMax = tempsMax.get(i).asDouble();
            double tempMin = tempsMin.get(i).asDouble();
            int weatherCode = weatherCodes.get(i).asInt();
            double windSpeed = windSpeeds.get(i).asDouble();
            int windDir = windDirs.get(i).asInt();

            String description = getWeatherDescription(weatherCode);
            DailyWeatherData dwd = new DailyWeatherData(date, tempMax, tempMin, windSpeed, windDir, description, "OpenMeteo");

            List<HourlyWeatherData> hourlyList = new ArrayList<>();
            for (int j = 0; j < timesHourly.size(); j++) {
                String hourTime = timesHourly.get(j).asText();
                if (hourTime.startsWith(date)) {
                    double temp = tempsHourly.get(j).asDouble();
                    int hCode = weatherCodesHourly.get(j).asInt();
                    double hWindSpeed = windSpeedsHourly.get(j).asDouble();
                    int hWindDir = windDirsHourly.get(j).asInt();
                    String desc = getWeatherDescription(hCode);

                    HourlyWeatherData hwd = new HourlyWeatherData(hourTime, temp, hWindSpeed, hWindDir, desc, "OpenMeteo");
                    hourlyList.add(hwd);
                }
            }
            dwd.hourlyData.addAll(hourlyList);
            result.add(dwd);
        }
        return result;
    }

}

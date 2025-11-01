package com.apitest2.apis;


import java.io.IOException;
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
public class WeatherApiCom implements  WeatherApi {

    @Value("${app.api.wethapico}")
    private String apikey;

    public String getApiKey() {
        return apikey;
    }

    @Override
    public void getAllWeather(String city) throws Exception {
        try {
            String location = city.trim().replace(" ", "%20");
            String url = "http://api.weatherapi.com/v1/forecast.json?key=" + apikey +
                        "&q=" + location + "&days=3&aqi=no&alerts=no";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            if (root.isEmpty()) {
                System.out.println("No data found.");
                return;
            }

            JsonNode loc = root.get("location");
            JsonNode current = root.get("current");

            System.out.println("\n[WeatherAPI.com]");
            System.out.println("Location: " + loc.get("name").asText() + ", " +
                            loc.get("region").asText() + ", " +
                            loc.get("country").asText());
            System.out.println("Local Time: " + loc.get("localtime").asText());
            System.out.println("Temperature: " + current.get("temp_c").asDouble() + "°C");
            System.out.println("Weather: " + current.get("condition").get("text").asText());
            System.out.println("Wind: " + current.get("wind_kph").asDouble() + " km/h " +
                            current.get("wind_dir").asText());
            System.out.println();

            JsonNode forecastDays = root.get("forecast").get("forecastday");
            for (JsonNode day : forecastDays) {
                String date = day.get("date").asText();
                JsonNode dayInfo = day.get("day");

                System.out.println("Date: " + date);
                System.out.println(" - Max/Min Temp: " +
                                dayInfo.get("maxtemp_c").asDouble() + "/" +
                                dayInfo.get("mintemp_c").asDouble() + "°C");
                System.out.println(" - Weather: " + dayInfo.get("condition").get("text").asText());
                System.out.println(" - Chance of Rain: " +
                                dayInfo.get("daily_chance_of_rain").asInt() + "%");
                System.out.println(" - Wind: " + dayInfo.get("maxwind_kph").asDouble() + " km/h");
                System.out.println();
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        try {
            String location = city.trim().replace(" ", "%20");
            String urlString = "http://api.weatherapi.com/v1/forecast.json?key=" + apikey +
                    "&q=" + location + "&days=3&aqi=no&alerts=no";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());
            if (json.isEmpty()) {
                System.out.println("no");
                return null;
            }

            // JsonNode loc = json.get("location");
            JsonNode current = json.get("current");
            double temp = current.get("temp_c").asDouble();
            String weatherDesc = current.get("condition").get("text").asText();
            if (weatherDesc.toLowerCase().contains("clear")) {
                weatherDesc = "sunny";
            }
            double wind = current.get("wind_kph").asDouble();
            int windDeg = current.get("wind_degree").asInt();

            // System.out.println("\n[WeatherAPI.com]");
            // System.out.println("Location: " + loc.getString("name") + ", " + loc.getString("region") + ", " + loc.getString("country"));
            // System.out.println("Local Time: " + loc.getString("localtime"));
            // System.out.println("Temperature: " + current.getDouble("temp_c") + "°C");
            // System.out.println("Weather: " + current.getJSONObject("condition").getString("text"));
            // System.out.println("Wind: " + current.getDouble("wind_kph") + " km/h " + current.getString("wind_dir"));
            // System.out.println();

            return new CurrentWeatherData(temp, wind, "WeatherApi", weatherDesc, windDeg);

        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
        // return new CurrentWeatherData(0, 0, city, city, 0);
        return null;
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        List<DailyWeatherData> result = new ArrayList<>();            
        try {
            String location = city.trim().replace(" ", "%20");
            String apiKey = apikey;
            String urlString = "http://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                    "&q=" + location + "&days=3&aqi=no&alerts=no";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            if (root.isEmpty()) {
                System.out.println("No data found.");
                return List.of();
            }

            JsonNode forecastDays = root.get("forecast").get("forecastday");
            for (JsonNode day : forecastDays) {
                // day = forecastDays.getJSONObject(i);
                String date = day.get("date").asText();
                JsonNode dayInfo = day.get("day");

                double maxTemp = dayInfo.get("maxtemp_c").asDouble();
                double minTemp = dayInfo.get("mintemp_c").asDouble();
                double maxWind = dayInfo.get("maxwind_kph").asDouble();
                String condition = dayInfo.get("condition").get("text").asText();

                JsonNode hours = day.get("hour");
                double totalSin = 0, totalCos = 0;
                int count = 0;

                List<HourlyWeatherData> hourlyList = new ArrayList<>();
                for (JsonNode hour : hours) {
                    // hour = hours.getJSONObject(j);
                    String time = hour.get("time").asText().replace(" ", "T");
                    double tempC = hour.get("temp_c").asDouble();
                    double windKph = hour.get("wind_kph").asDouble();
                    int windDeg = hour.get("wind_degree").asInt();
                    String hourCondition = hour.get("condition").get("text").asText();
                    if (hourCondition.equalsIgnoreCase("clear")) {
                        hourCondition = "sunny";
                    }

                    HourlyWeatherData hwd = new HourlyWeatherData(time, tempC, windKph, windDeg, hourCondition, "WeatherApi");
                    hourlyList.add(hwd);

                    double rad = Math.toRadians(windDeg);
                    totalSin += Math.sin(rad);
                    totalCos += Math.cos(rad);
                    count++;
                }

                /*
                for (int j = 0; j < hours.length(); j++) {
                    int windDegree = hours.getJSONObject(j).getInt("wind_degree");
                    double rad = Math.toRadians(windDegree);
                    totalSin += Math.sin(rad);
                    totalCos += Math.cos(rad);
                    count++;
                }
                 */

                int avgWindDir = -1;
                if (count > 0) {
                    double avgRad = Math.atan2(totalSin / count, totalCos / count);
                    if (avgRad < 0) avgRad += 2 * Math.PI;
                    avgWindDir = (int) Math.round(Math.toDegrees(avgRad));
                }

                DailyWeatherData dwd = new DailyWeatherData(
                        date, maxTemp, minTemp, maxWind, avgWindDir, condition,
                        "WeatherApi", hourlyList);
                result.add(dwd);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
        return result;
    }
}

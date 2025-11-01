package com.apitest2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.apitest2.apis.*;
import com.apitest2.datixd.*;

@Repository
public class WeatherRepository {
    
    private final List<WeatherApi> apis;

    @Autowired
    public WeatherRepository(WeatherApiCom w1, IlMeteoScraper w2, OpenMeteoAPI w3, VisualCrossingAPI w4) {
        this.apis = List.of(w1, w2, w3, w4);
    }

    // @Autowired
    // public WeatherRepository(OpenMeteoAPI openMeteoAPI, AccuWeatherAPI accuWeatherAPI) {
    //     this.apis = List.of(openMeteoAPI, accuWeatherAPI);  }
    
    //private final List<WeatherApi> apis = new ArrayList<>();
    // @Autowired
    // public WeatherRepository(VisualCrossingAPI visualCrossing) {
    //     // apis.add(new IlMeteoScraper());
    //     // apis.add(new AccuWeatherAPI());
    //     apis.add(new VisualCrossingAPI());
    //     // apis.add(new OpenMeteoAPI());
    //     // apis.add(new WeatherApiCom());  }

    public CurrentWeatherData getCurrentWeatherFromAll(String city) throws Exception {
        List<CurrentWeatherData> collected = new ArrayList<>();
        System.out.println("------------------------------------");
        System.out.println(city);
        System.out.println();
        for (WeatherApi api : apis) {
            try {
                CurrentWeatherData data = api.getCurrentWeather(city);
                if (data == null) {
                    System.out.println("no data xdd");
                    continue;
                }
                collected.add(data);
            } catch (Exception e) {
                System.out.println("\nError: " + api.getClass().getSimpleName() + e);
            }
        }
        if (collected.isEmpty()) {
            System.out.println("No current data");
            return null;     }

        for (CurrentWeatherData d : collected) {
            System.out.println();
            System.out.printf("%-15s | Temp: %5.1f°C | Wind: %4.1f kmh %3d° (%s) | %s\n",
                    d.source, d.temperature, d.windSpeed, d.windDir,
                    windDirectionToCompass(d.windDir), d.weatherDescription);   }

        CurrentWeatherData avg = calculateAverage(collected);
        System.out.println();
        System.out.println(" - Average Current Weather: "
                + "T: " + String.format("%.1f", avg.temperature) + "°C "
                + " | Wind: " + String.format("%.1f", avg.windSpeed) + "kmh "
                + windDirectionToCompass(avg.windDir)
                + " | " + avg.weatherDescription);
        offsetCheck(collected, avg);
        return avg;
    }

    public List<DailyWeatherData> getDailyWeatherFromAll(String city) throws Exception {
        List<DailyWeatherData> collected = new ArrayList<>();
        System.out.println("------------------------------------");
        System.out.println(city);
        System.out.println();
        for (WeatherApi api : apis) {
            try {
                List<DailyWeatherData> data = api.getDailyForecast(city);
                if (data == null) {
                    System.out.println("no data xdd");
                    continue;
                }
                collected.addAll(data);
            } catch (Exception e) {
                System.out.println("\nError: " + api.getClass().getSimpleName());
            }
        }
        if (collected.isEmpty()) {
            System.out.println("No current data");
            return null;     }

        String previousSource = "";
        for (DailyWeatherData d : collected) {
            if (!d.source.equals(previousSource)) {
                System.out.println("\n" + d.source + " data:");
                previousSource = d.source;  }

            if (d.windDir < 0) {
                System.out.printf("%-10s | T: %4.1f/%4.1f°C | %skmh  ?  | %s\n",
                        d.date, d.temperature, d.TempMin, d.windSpeed, d.weatherDescription);
            } else {
                System.out.printf("%-10s | T: %4.1f/%4.1f°C | %skmh %-4s | %s\n",
                        d.date, d.temperature, d.TempMin, d.windSpeed, windDirectionToCompass(d.windDir), d.weatherDescription);
            }

            if (d.hourlyData != null && !d.hourlyData.isEmpty()) {
                System.out.println("  Hourly:");
                /*for (HourlyWeatherData h : d.hourlyData) {
                    String timeOnly = h.dateTime.split("T")[1].split(":00")[0];
                    String hourFormatted = timeOnly + ":00";
                    System.out.printf("    %s | %.1f°C | %skmh %-4s | %s\n",
                            hourFormatted, h.temperature, h.windSpeed, windDirectionToCompass(h.windDir), h.weatherDescription);
                } */ 
            }
        }

        List<DailyWeatherData> avgPerDay = calculateDailyAverages(collected);
        avgPerDay.sort(Comparator.comparing(d -> d.date));
        System.out.println("\n - Average per day:");
        for (DailyWeatherData avg : avgPerDay) {
            System.out.printf("%-10s | T: %.1f/%.1f°C | %.1fkmh %-4s | %s\n",
                    avg.date, avg.temperature, avg.TempMin,
                    avg.windSpeed, windDirectionToCompass(avg.windDir), avg.weatherDescription);

            /*for (HourlyWeatherData h : avg.hourlyData) {
                String timeOnly = h.dateTime.split("T")[1].split(":00")[0];
                String hourFormatted = timeOnly + ":00";
                System.out.printf("    %s | %.1f°C | %.1fkmh %-4s | %s\n",
                        hourFormatted, h.temperature, h.windSpeed,
                        windDirectionToCompass(h.windDir), h.weatherDescription);
            }*/
        }
        // dailyOffsetCheck(collected, avgPerDay);
        return avgPerDay;
    }





    static void offsetCheck(List<CurrentWeatherData> data, CurrentWeatherData avg) {
        double tempThreshold = 2.0;  // degrees Celsius difference allowed
        double windThreshold = 5.0;  // km/h difference allowed
        double dirThreshold = 40.0;  // degrees allowed deviation for wind dir
        boolean outlierFlag = false;

        for (CurrentWeatherData d : data) {
            boolean tempOutlier = Math.abs(d.temperature - avg.temperature) > tempThreshold;
            boolean windOutlier = Math.abs(d.windSpeed - avg.windSpeed) > windThreshold;

            boolean dirOutlier = false;
            if (avg.windDir >= 0 && d.windDir >= 0) {
                double diff = Math.abs(d.windDir - avg.windDir);
                // wind direction is circular: 350° vs 10° -> only 20° apart
                diff = Math.min(diff, 360 - diff);
                dirOutlier = diff > dirThreshold;
            }

            if (tempOutlier || windOutlier || dirOutlier) {
                System.out.println("Warning: " + d.source + " has outlier data!");
                outlierFlag = true;
            }
        }
        if (!outlierFlag) {
            // System.out.println("\nNo outlier data");
        }
    }

    @SuppressWarnings("unused")
    static List<DailyWeatherData> calculateDailyAverages(List<DailyWeatherData> data) {
        Map<String, List<DailyWeatherData>> grouped = new HashMap<>();
        for (DailyWeatherData d : data) {
            grouped.computeIfAbsent(d.date, k -> new ArrayList<>()).add(d);
        }

        Map<String, List<HourlyWeatherData>> hourlyMap = calculateHourlyAverages(data);
        List<DailyWeatherData> averages = new ArrayList<>();
        for (String date : grouped.keySet()) {
            List<DailyWeatherData> sameDay = grouped.get(date);

            double sumMax = 0, sumMin = 0, sumWind = 0;

            for (DailyWeatherData d : sameDay) {
                sumMax += d.temperature;
                sumMin += d.TempMin;
                sumWind += d.windSpeed;
            }

            double avgMax = sumMax / sameDay.size();
            double avgMin = sumMin / sameDay.size();
            double avgWind = sumWind / sameDay.size();
            int avgDir = averageDailyWindDirection(sameDay);
            String finalDesc = mergeDailyWeatherDescriptions(sameDay);
            List<HourlyWeatherData> hourly = hourlyMap.getOrDefault(date, new ArrayList<>());

            averages.add(new DailyWeatherData(
                    date, avgMax, avgMin, Math.round(avgWind * 10.0) / 10.0, avgDir, finalDesc, "Average", hourly
            ));
        }
        return averages;
    }

    static CurrentWeatherData calculateAverage(List<CurrentWeatherData> data) {
        double sumTemp = 0;
        double sumWind = 0;
        boolean isNight = false;

        for (CurrentWeatherData d : data) {
            sumTemp += d.temperature;
            sumWind += d.windSpeed;

            if (d.isNight) {
                isNight = true;
            }
        }

        double avgTemp = sumTemp / data.size();
        double avgWind = sumWind / data.size();
        int avgWindDir = averageWindDirection(data);
        String avgDesc = mergeWeatherDescriptions(data);

        return new CurrentWeatherData(avgTemp, avgWind, "Average", avgDesc, avgWindDir, isNight);
    }

    static int averageWindDirection(List<CurrentWeatherData> data) {
        double sumSin = 0.0;
        double sumCos = 0.0;
        int totalWeight = 0;

        for (CurrentWeatherData d : data) {
            if (d.windDir >= 0) {
                double radians = Math.toRadians(d.windDir);
                int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                sumSin += Math.sin(radians) * weight;
                sumCos += Math.cos(radians) * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) return -10; // no valid data

        double avgRadians = Math.atan2(sumSin / totalWeight, sumCos / totalWeight);
        double avgDegrees = Math.toDegrees(avgRadians);
        if (avgDegrees < 0) avgDegrees += 360;

        return (int) Math.round(avgDegrees);
    }

    static String mergeWeatherDescriptions(List<CurrentWeatherData> data) {
        double total = 0;
        int totalWeight = 0;
        System.out.println();
        for (CurrentWeatherData d : data) {
            if (d.weatherDescription != null) {
                String desc = d.weatherDescription.toLowerCase().trim();

                Integer score = null;
                for (String key : DESCRIPTION_SCORES.keySet()) {
                    if (desc.contains(key)) {
                        score = DESCRIPTION_SCORES.get(key);
                        break;
                    }
                }

                System.out.println("score: " + score);
                if (score != null) {
                    int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                    total += score * weight;
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight == 0) return "Unknown";
        int avgScore = (int) Math.floor(total / totalWeight);
        System.out.println("avgscore = " + avgScore);
        return scoreToDescription(avgScore);
    }

    static int averageDailyWindDirection(List<DailyWeatherData> data) {
        double sumSin = 0.0;
        double sumCos = 0.0;
        int totalWeight = 0;

        for (DailyWeatherData d : data) {
            if (d.windDir >= 0) {
                double radians = Math.toRadians(d.windDir);
                int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                sumSin += Math.sin(radians) * weight;
                sumCos += Math.cos(radians) * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) return -10; // no valid wind dir

        double avgRadians = Math.atan2(sumSin / totalWeight, sumCos / totalWeight);
        double avgDegrees = Math.toDegrees(avgRadians);
        if (avgDegrees < 0) avgDegrees += 360;

        return (int) Math.round(avgDegrees);
    }

    static String mergeDailyWeatherDescriptions(List<DailyWeatherData> data) {
        double total = 0;
        int totalWeight = 0;

        for (DailyWeatherData d : data) {
            if (d.weatherDescription != null) {
                String desc = d.weatherDescription.toLowerCase().trim();

                Integer score = null;
                for (String key : DESCRIPTION_SCORES.keySet()) {
                    if (desc.contains(key)) {
                        score = DESCRIPTION_SCORES.get(key);
                        break;
                    }
                }

                if (score != null) {
                    int weight = d.source.equalsIgnoreCase("IlMeteo") ? 3 : 1;
                    total += score * weight;
                    totalWeight += weight;
                }
            }
        }

        if (totalWeight == 0) return "Unknown";
        int avgScore = (int) Math.floor(total / totalWeight);
        return scoreToDescription(avgScore);
    }

    @SuppressWarnings("unused")
    static Map<String, List<HourlyWeatherData>> calculateHourlyAverages(List<DailyWeatherData> dailyData) {
        Map<String, Map<String, List<HourlyWeatherData>>> grouped = new HashMap<>();

        for (DailyWeatherData day : dailyData) {
            if (day.hourlyData == null) continue;
            for (HourlyWeatherData h : day.hourlyData) {
                String[] parts = h.dateTime.split("T");
                String date = parts[0];
                String hour = parts[1].substring(0, 2); // 00, 01, ..., 23

                grouped
                        .computeIfAbsent(date, k -> new HashMap<>())
                        .computeIfAbsent(hour, k -> new ArrayList<>())
                        .add(h);
            }
        }

        Map<String, List<HourlyWeatherData>> result = new HashMap<>();
        for (String date : grouped.keySet()) {
            List<HourlyWeatherData> averagedPerHour = new ArrayList<>();

            for (String hour : grouped.get(date).keySet()) {
                List<HourlyWeatherData> entries = grouped.get(date).get(hour);

                double sumTemp = 0, sumWind = 0;
                int count = entries.size();

                List<DailyWeatherData> asDaily = new ArrayList<>();
                for (HourlyWeatherData h : entries) {
                    sumTemp += h.temperature;
                    sumWind += h.windSpeed;

                    // Convert to dummy DailyWeatherData to reuse helper functions
                    asDaily.add(new DailyWeatherData(
                            date, 0, 0, h.windSpeed, h.windDir, h.weatherDescription, h.source
                    ));
                }

                double avgTemp = sumTemp / count;
                double avgWind = sumWind / count;
                int avgDir = averageDailyWindDirection(asDaily);
                String avgDesc = mergeDailyWeatherDescriptions(asDaily);

                HourlyWeatherData averaged = new HourlyWeatherData(
                        date + "T" + hour + ":00:00",
                        avgTemp, avgWind, avgDir, avgDesc, "Average"
                );

                averagedPerHour.add(averaged);
            }

            averagedPerHour.sort(Comparator.comparing(h -> h.dateTime));
            result.put(date, averagedPerHour);
        }

        return result;
    }

    private static final Map<String, Integer> DESCRIPTION_SCORES = new HashMap<>();
    static {
        DESCRIPTION_SCORES.put("sunny", -1);
        DESCRIPTION_SCORES.put("clear", -1);
        DESCRIPTION_SCORES.put("partly cloudy", 1);
        DESCRIPTION_SCORES.put("mostly cloudy", 2);
        DESCRIPTION_SCORES.put("cloudy", 2);
        DESCRIPTION_SCORES.put("overcast", 2);
        DESCRIPTION_SCORES.put("rain", 3);
        DESCRIPTION_SCORES.put("drizzle", 3);
        DESCRIPTION_SCORES.put("showers", 4);
        DESCRIPTION_SCORES.put("thunderstorm", 5);
        DESCRIPTION_SCORES.put("snow", 8);
    }

    private static String scoreToDescription(int score) {
            if (score <= 0) return "Sunny";
            if (score == 1) return "Partly cloudy";
            if (score == 2) return "Cloudy";
            if (score == 3) return "Rain";
            if (score == 4) return "Showers";
            if (score == 5) return "Thunderstorm";
            if (score >= 6) return "Snow";
            return "Unknown";
        }

    public static String windDirectionToCompass(double degrees) {
        if (degrees < 0) return "N/A";
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

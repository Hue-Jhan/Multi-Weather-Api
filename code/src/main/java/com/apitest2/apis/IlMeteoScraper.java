package com.apitest2.apis;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.apitest2.datixd.CurrentWeatherData;
import com.apitest2.datixd.DailyWeatherData;
import com.apitest2.datixd.HourlyWeatherData;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IlMeteoScraper implements WeatherApi {

    @Override
    public void getAllWeather(String city) throws Exception {
        
    }

    @Override
    public CurrentWeatherData getCurrentWeather(String city) throws Exception {
        boolean isNight = false;
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";
        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        if (days.size() <= 1)
            throw new Exception("No forecast days found.");

        Element today = days.get(1);
        Element linkElement = today.selectFirst("a");
        if (linkElement == null)
            throw new Exception("No link for today found.");

        String link = linkElement.attr("href");
        String dayUrl = link.startsWith("http") ? link : baseUrl + link;
        Document docDay = Jsoup.connect(dayUrl).get();
        Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h");
        if (rows.isEmpty())
            throw new Exception("No hourly rows found.");

        Element row = rows.get(0);
        Elements tds = row.select("td");

        double temperature = 0;
        double windSpeed = 0;
        int windDir = -1;
        int simbolo = -1;
        String description = "Unknown";

        Element simboloSpan = row.selectFirst("span[data-simbolo]");
        if (simboloSpan != null) {
            try {
                simbolo = Integer.parseInt(simboloSpan.attr("data-simbolo"));
                description = getWeatherDescription(simbolo);
                if (description.toLowerCase().contains("night")) isNight = true;
            } catch (NumberFormatException ignored) {}
        }

        // Temperature
        if (tds.size() > 2) {
            Element tempEl = tds.get(2).selectFirst("span.temp_cf");
            if (tempEl != null) {
                String tempText = tempEl.text().replace("°", "").trim();
                try {
                    temperature = Double.parseDouble(tempText);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Wind speed
        if (tds.size() > 5) {
            Element windTd = tds.get(5).selectFirst("span.boldval.wind_kmkn");
            if (windTd != null) {
                try {
                    windSpeed = Double.parseDouble(windTd.text().trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        // Wind direction (from rotation angle)
        if (tds.size() > 4) {
            Element windDirDiv = tds.get(4).selectFirst("div.w[class*=wind]");
            if (windDirDiv != null) {
                Matcher matcher = Pattern.compile("rotate\\((\\d+(?:\\.\\d+)?)deg\\)").matcher(windDirDiv.attr("style"));
                if (matcher.find()) {
                    try {
                        windDir = (int) Double.parseDouble(matcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return new CurrentWeatherData(temperature, windSpeed, "IlMeteo", description, windDir, isNight);
    }

    @Override
    public List<DailyWeatherData> getDailyForecast(String city) throws Exception {
        String formattedCity = city.toLowerCase().replace(" ", "+");
        String baseUrl = "https://www.ilmeteo.it";
        Document doc = Jsoup.connect(baseUrl + "/meteo/" + formattedCity).get();
        Elements days = doc.select(".forecast_day_selector__list__item");

        if (days.size() <= 1) {
            throw new Exception("No forecast days found."); }

        List<DailyWeatherData> result = new ArrayList<>();
        for (int i = 1; i < days.size(); i++) {
            @SuppressWarnings("unused")
            boolean isNight = false;
            try {
                Element day = days.get(i);
                Element linkElement = day.selectFirst("a");
                Element conditionElement = day.selectFirst(".s-small-container-all");
                String description = "Unknown";
                int simboloCode = -1;

                if (conditionElement != null) {
                    Element simboloElement = conditionElement.selectFirst("[data-simbolo]");
                    if (simboloElement != null) {
                        try {
                            simboloCode = Integer.parseInt(simboloElement.attr("data-simbolo"));
                            description = getWeatherDescription(simboloCode);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (description.toLowerCase().contains("night")) isNight = true;
                }

                if (linkElement == null) continue;

                String link = linkElement.attr("href");
                String dayUrl = link.startsWith("http") ? link : baseUrl + link;

                String linkText = linkElement.text().trim(); // e.g. "Lun 21 24° 33°"
                String[] parts = linkText.split(" ");
                if (parts.length < 4) continue;

                String rawDate = parts[0] + " " + parts[1];
                String formattedDate = parseIlMeteoDateAuto(rawDate);
                double tempMin = Double.parseDouble(parts[2].replace("°", ""));
                double tempMax = Double.parseDouble(parts[3].replace("°", ""));

                List<HourlyWeatherData> hourlyList = new ArrayList<>();
                Document docDay = Jsoup.connect(dayUrl).get();
                Elements rows = docDay.select("tr.forecast_1h, tr.forecast_3h:not(.hidden)");

                if (rows.isEmpty()) continue;

                LocalDate localDate = LocalDate.parse(formattedDate, DateTimeFormatter.ISO_LOCAL_DATE);
                double totalWindSpeed = 0;
                double totalDirSin = 0;
                double totalDirCos = 0;
                int count = 0;

                for (Element rowEl : rows) {
                    Elements tds = rowEl.select("td");
                    if (tds.size() <= 5) continue;

                    try {
                        String hourStr = rowEl.attr("data-hour");
                        int hourInt = Integer.parseInt(hourStr);
                        String dateTime = String.format("%sT%02d:00", localDate, hourInt);

                        Element tempEl = rowEl.selectFirst("span.temp_cf");
                        double temperature = Double.parseDouble(tempEl.text().replace(",", "."));

                        Element symbolEl = rowEl.selectFirst("span.s-small");
                        int simbolo = Integer.parseInt(symbolEl.attr("data-simbolo"));
                        String weatherDesc = getWeatherDescription(simbolo);

                        Element windTd = tds.get(5).selectFirst("span.boldval.wind_kmkn");
                        double windSpeed = windTd != null ? Double.parseDouble(windTd.text()) : 0;
                        totalWindSpeed += windSpeed;

                        int dirDeg = -1;
                        Element windDirDiv = tds.get(4).selectFirst("div.w[class*=wind]");
                        if (windDirDiv != null) {
                            Matcher matcher = Pattern.compile("rotate\\((\\d+(?:\\.\\d+)?)deg\\)").matcher(windDirDiv.attr("style"));
                            if (matcher.find()) {
                                dirDeg = (int) Double.parseDouble(matcher.group(1));
                                double rad = Math.toRadians(dirDeg);
                                totalDirSin += Math.sin(rad);
                                totalDirCos += Math.cos(rad);
                            }
                        }
                        count++;
                        hourlyList.add(new HourlyWeatherData(dateTime, temperature, windSpeed, dirDeg, weatherDesc, "IlMeteo"));

                    } catch (Exception e) {
                        System.out.println("Could not parse row: " + e.getMessage());
                    }
                }

                double avgWindSpeed = count > 0 ? Math.round((totalWindSpeed / count) * 10.0) / 10.0 : 0;
                int avgWindDir = -1;
                if (count > 0) {
                    double avgRad = Math.atan2(totalDirSin / count, totalDirCos / count);
                    if (avgRad < 0) avgRad += 2 * Math.PI;
                    avgWindDir = (int) Math.round(Math.toDegrees(avgRad)) % 360;
                }

                DailyWeatherData dwd = new DailyWeatherData(formattedDate, tempMax, tempMin, avgWindSpeed, avgWindDir, description, "IlMeteo");
                dwd.hourlyData.addAll(hourlyList);
                result.add(dwd);

            } catch (Exception e) {
                System.out.println("Skipping day " + i + " due to error: " + e.getMessage());
            }
        }
        return result;
    }


    public static String parseIlMeteoDateAuto(String dayStr) {
        String[] parts = dayStr.split(" ");
        if (parts.length != 2) return null;
        String dow = parts[0].trim();
        int dayOfMonth = Integer.parseInt(parts[1]);
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        if (dayOfMonth < today.getDayOfMonth() - 10) {
            month++;
            if (month > 12) {
                month = 1;
                year++; } }

        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        DayOfWeek actualDow = date.getDayOfWeek();
        String dowShort = getItalianShortDayOfWeek(actualDow);

        if (!dowShort.equalsIgnoreCase(dow)) {
            System.out.println("Warning: parsed DOW does not match! " + dow + " vs " + dowShort);
        }

        return date.toString();
    }

    private static String getWeatherDescription(int code) {
        boolean isNight = false;

        if (code >= 100) {
            isNight = true;
            code -= 100;
        }

        String description;

        if (code == 1) {
            description = "Sunny";
        } else if (code == 2) {
            description = "Mostly Sunny";
        } else if (code == 3) {
            description = "Partly Cloudy";
        } else if (code == 4 || code == 8) {
            description = "Cloudy";
        } else if (code >= 5 && code <= 9) {
            description = "Rain";
        } else if (code >= 10 && code <= 12) {
            description = "Showers";
        } else if (code >= 13 && code <= 19) {
            description = "Thunderstorm";
        } else {
            description = "Unknown";
        }

        if (isNight) {
            description += " Night";
        }

        return description;
    }

    public static String getItalianShortDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Lun";
            case TUESDAY -> "Mar";
            case WEDNESDAY -> "Mer";
            case THURSDAY -> "Gio";
            case FRIDAY -> "Ven";
            case SATURDAY -> "Sab";
            case SUNDAY -> "Dom";
            default -> "?";
        };
    }

}

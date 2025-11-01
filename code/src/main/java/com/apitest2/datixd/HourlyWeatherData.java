package com.apitest2.datixd;

public class HourlyWeatherData extends WeatherData {
    public String dateTime;  // e.g., "2025-07-19T15:00"

    public HourlyWeatherData(String dateTime, double temperature,
                             double windSpeed, int windDir,
                             String weatherDescription, String source) {
                                
        super(temperature, windSpeed, source, weatherDescription, windDir);
        this.dateTime = dateTime;
    }
}

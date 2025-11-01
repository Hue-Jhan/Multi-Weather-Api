package com.apitest2.datixd;

public class CurrentWeatherData extends WeatherData {
    
    public boolean isNight = false;

    public CurrentWeatherData(double temperature, double windSpeed, String source,
                              String weatherDescription, int windDir) {
        
        super(temperature, windSpeed, source, weatherDescription, windDir);
    }

    public CurrentWeatherData(double temperature, double windSpeed, String source,
                              String weatherDescription, int windDir, boolean isNight) {
        
        super(temperature, windSpeed, source, weatherDescription, windDir);
        this.isNight = isNight;
    }
}

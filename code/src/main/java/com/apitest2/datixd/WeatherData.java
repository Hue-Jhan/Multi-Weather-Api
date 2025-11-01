package com.apitest2.datixd;

public abstract class WeatherData {
    public double temperature;  // in °C (avg in Current/Hourly, Max in Daily)
    public double windSpeed;    // in km/h (avg in Current/Hourly, Max in Daily)
    public String source;       // API name for reference
    public String weatherDescription;  // sunny/cloudy/etc
    public int windDir;  // in degrees (°), direction of wind, some API do not include it

    public WeatherData(double temperature, double windSpeed, String source,
                       String weatherDescription, int windDir) {
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.source = source;
        this.weatherDescription = weatherDescription;
        this.windDir = windDir;
    }
}

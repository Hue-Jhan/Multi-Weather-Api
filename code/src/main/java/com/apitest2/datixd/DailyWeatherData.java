package com.apitest2.datixd;
import java.util.ArrayList;
import java.util.List;

public class DailyWeatherData extends WeatherData {
    public String date;
    public double TempMin;

    public List<HourlyWeatherData> hourlyData;

    public DailyWeatherData(String date, double temperature, double TempMin,
                            double windSpeed, int windDir, String weatherDescription,
                            String source, List<HourlyWeatherData> hourlyData) {
        
        super(temperature, windSpeed, source, weatherDescription, windDir);
        this.date = date;
        this.TempMin = TempMin;
        this.hourlyData = hourlyData != null ? hourlyData : new ArrayList<>();
    }

    // without hourly data
    public DailyWeatherData(String date, double temperature,
                            double TempMin, double windSpeed, 
                            int windDir, String weatherDescription, String source) {
        
        super(temperature, windSpeed, source, weatherDescription, windDir);
        this.date = date;
        this.TempMin = TempMin;
        this.hourlyData = new ArrayList<>();
    }

    public boolean hasHourlyData() {
        return hourlyData != null && !hourlyData.isEmpty();
    }
}

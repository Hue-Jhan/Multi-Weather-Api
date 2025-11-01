package com.apitest2.apis;
import com.apitest2.datixd.*;
import java.util.List;

public interface WeatherApi {
    
    void getAllWeather(String city) throws Exception;
    CurrentWeatherData getCurrentWeather(String city) throws Exception;
    List<DailyWeatherData> getDailyForecast(String city) throws Exception;
}

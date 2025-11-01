package com.apitest2;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitest2.datixd.CurrentWeatherData;
import com.apitest2.datixd.DailyWeatherData;
import com.apitest2.database.*;

import jakarta.servlet.http.*;

@RestController
@RequestMapping("/api/")
public class WeatherController {

    private final WeatherRepository weatherRepo;
    private final LoggingService loggingService;

    @Autowired
    public WeatherController(WeatherRepository weatherRepo, LoggingService loggingService) {
        this.weatherRepo = weatherRepo;
        this.loggingService = loggingService;   }

    @GetMapping("/current")
    public ResponseEntity<CurrentWeatherData> getCurrentWeather(@RequestParam String city, HttpServletRequest request) {
        long start = System.currentTimeMillis();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();

        HttpStatus status = null;
        try {
            var result = weatherRepo.getCurrentWeatherFromAll(city);
            loggingService.log(ip, city, "/api/current", 200);
            status = HttpStatus.OK;
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.log(ip, city, "/api/current", 500);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(status).build();
        } finally {
            long end = System.currentTimeMillis();
            String log = String.format(
                    "[%s] /api/current IP=%s city=%s status=%d time=%dms",
                    java.time.LocalDateTime.now(), ip, city, status.value(), end - start
            );
            // saveLog(log);
        }
    }

    @GetMapping("/forecast")
    public ResponseEntity<List<DailyWeatherData>> getForecast(
            @RequestParam String city,
            HttpServletRequest request) {

        long start = System.currentTimeMillis();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();

        HttpStatus status = null;
        try {
            var result = weatherRepo.getDailyWeatherFromAll(city);
            loggingService.log(ip, city, "/api/current", 200);
            status = HttpStatus.OK;
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            loggingService.log(ip, city, "/api/current", 500);
            return ResponseEntity.status(status).build();
        } finally {
            long end = System.currentTimeMillis();
            String log = String.format(
                    "[%s] /api/forecast IP=%s city=%s status=%d time=%dms",
                    java.time.LocalDateTime.now(), ip, city, status.value(), end - start
            );
            saveLog(log);
        }
    }

    @SuppressWarnings({"CallToPrintStackTrace", "UseSpecificCatch"})
    private void saveLog(String log) {
        System.out.println();
        System.out.println(log);
        try {
            String jarDir = new File(System.getProperty("java.class.path")).getParent();
            File logFile = new File(jarDir, "requests.log");
            try (var writer = new FileWriter(logFile, true)) {
                writer.write(log + System.lineSeparator());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

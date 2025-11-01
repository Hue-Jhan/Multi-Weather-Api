package com.apitest2.database;

import java.io.FileWriter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {

    private final JdbcTemplate jdbc;

    public LoggingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String ip, String city, String endpoint, int statusCode) {
        RequestLog log = new RequestLog(ip, city, endpoint, statusCode);

        System.out.println(log.toString());
        try (FileWriter writer = new FileWriter("/app/logs/requests.log", true)) {
            writer.write(log.toString() + "\n");
            System.out.println("+ added to log file ");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            jdbc.update("INSERT INTO request_logs (ip, city, endpoint, status_code, timestamp) VALUES (?, ?, ?, ?, ?)",
                    log.getIp(), log.getCity(), log.getEndpoint(), log.getStatusCode(), log.getTimestamp());
            System.out.println("+ added to db");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
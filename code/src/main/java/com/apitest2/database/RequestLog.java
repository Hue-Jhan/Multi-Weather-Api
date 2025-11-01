package com.apitest2.database;

import java.time.LocalDateTime;

// @Entity
// @Table(name = "request_logs")
public class RequestLog {
    //  @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    // private Long id;
    private String ip;
    private String city;
    private String endpoint;
    private int statusCode;
    private LocalDateTime timestamp;

    public RequestLog(String ip, String city, String endpoint, int statusCode) {
        this.ip = ip;
        this.city = city;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return timestamp + " | " + ip + " | " + city + " | " + endpoint + " | " + statusCode;
    }

    // Getters (optional)
    public String getIp() { return ip; }
    public String getCity() { return city; }
    public String getEndpoint() { return endpoint; }
    public int getStatusCode() { return statusCode; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
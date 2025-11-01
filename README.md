# Multi Weather Api
Spring boot framework API that compares multiple Weather APIs/scrapers to deliver the best possible forecast, and shows daily/hourly data from each API.

# How to run

### 1) Requirements
Docker, Docker Compose, Java 23+, Maven, and a ```.env``` file with active working APIs.

### 2) Compiling the app 
```cd Multi-Weather-Api/code```

```docker build -t weather-api:latest . ```

```docker compose build```

```docker compose up```


The API will be available at http://localhost:8080/api/, the  PostgreSQL instance  at localhost:5432; you can port forward with
```ngrok http 8080``` once you add the ngrok authtoken.

### 3) Checking logs and db

You can connect to the DB manually with ```docker exec -it weather_db psql -U weath_api_logger -d weather_logs``` in the db container terminal, then with ```SELECT * FROM request_logs;``` you can view user info such as IPs, requests, api data, etc.

# 💻 Code
a

e

i


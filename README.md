# Multi Weather Api
Spring boot framework API that compares multiple Weather APIs/scrapers to deliver the best possible forecast, and shows daily/hourly data from each API.

# ⚙ How to run


<img src="media/wapi_forecast.png" align="right" width=300>

### 1) Requirements
Docker, Docker Compose, Java 23+, Maven.

You will also need a ```.env``` file and an updated ```application.properties``` with with active working APIs.

### 2) Compiling the app 

```cd Multi-Weather-Api/code```


<img src="media/wapi_terminal_logs.png" align="right" width=400>


```./mvnw clean package -DskipTests```

```docker build -t weather-api:latest . ```

```docker compose build```

```docker compose up```



The API will be available at http://localhost:8080/api/, the  PostgreSQL instance  at localhost:5432; you can port forward with
```ngrok http 8080``` once you add the ngrok authtoken.


### 3) Checking logs and db  <img src="media/wapi_logs.png" align="right" width=300>

You can connect to the DB manually with ```docker exec -it weather_db psql -U weath_api_logger -d weather_logs``` in the db container terminal, then with ```SELECT * FROM request_logs;``` you can view user info such as IPs, requests, api data, etc.

# 💻 Code & APIs

The backend is a light rework of my previous repo (Multi Weather App)[https://github.com/Hue-Jhan/Multi-Weather-App]. The APIs i used are:

- GeoCodeMaps Api ([here](https://geocode.maps.co/));
- VisualCrossing Api ([here](https://www.visualcrossing.com/weather-query-builder/)), international;
- AccuWeather Api, ([here](https://developer.accuweather.com/)), international but requires location key from each city, which requires its Nation code, because of this i set this Api for italian cities only;
- OpenMeteo Api ([here](https://open-meteo.com/en/docs)), international;
- WeatherApi.com Api ([here](https://www.weatherapi.com/)), international;
- IlMeteo (scraper, [here](http://ilmeteo.it/meteo/)), only works for Italy and some general european locations;
- Windy (work in progress Api, [here](https://api.windy.com/)), international.


The code makes requests to the APIs to extract the current weather data, and the forecast data for the next few days, including hourly data. The weather informations are then saved into custom data types, an average result is calculated and displayed to the user. The daily data is saved into storage and updated every 2 hours for efficiency (and to not waste api requests).

Here's a video example of me adding Milan as a location, keep in mind that you can only add cities in italy because some of the apis i used only work for italy. You can fix this by changing some of the remaining apis and adding your local weather apis.


### 📊 Average Data Algorithm

When calculating the average weather condition (like cloudy or sunny), getting the "best median result" out of all apis is quite a difficult job because some apis are less accurate than others or give entirely different results, so what i did was using custom maps, dictionaries, other data types and algorithms to convert weather info into score points (i assigned more points to IlMeteo cuz its the best italian weather system imo), then calculating an average result out of those points, here are some examples:

- Cloudy = 2, so if 4 apis say Cloudy and the fifth one says 3 (rainy), the median is [(2x4)+3]/5 =^ 2, which means the final result will be Cloudy;
- Sunny = -1, so if 3 apis say Sunny and 2 say Showers (4), the median is more or less 1, which means the final result will be Cloudy, however it's highly unlikely that apis give such different conditions for the same location. 

Why isn't Sunny 0? because statistically (i mean from what i've experienced) if the weather says it's going to be a Sunny day, there aren't going to be sudden changes in the forecast and the weather will be almost certainly sunny through the whole day, so i might as well say -1 or -5 cuz the result will most likely not change, while if the forecast says cloudy then there might be chanches of rain, which means the calculations have to be more precise to accurately predict weather it's going to rain or not. I did not include the "chanche of rain" variable in the data types, however EVERY api i used has this value, if you want you can modify the entire code, fetch this value and add it to the data types. 

The average wind direction is calculated using radians of course, then turned into degrees and the string corresponding to the name of the direction (eg SSW for south-south-west). The remaining data like temperature and windspeed is a simple median calculation.



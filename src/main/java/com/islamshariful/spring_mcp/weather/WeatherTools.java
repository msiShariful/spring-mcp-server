package com.islamshariful.spring_mcp.weather;

import com.islamshariful.spring_mcp.weather.OpenMeteoClient.Current;
import com.islamshariful.spring_mcp.weather.OpenMeteoClient.Daily;
import com.islamshariful.spring_mcp.weather.OpenMeteoClient.Location;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP tools for weather. Each @McpTool method becomes a tool the AI can call.
 * The AI reads the name and description to decide WHEN to call it,
 * and the @McpToolParam descriptions to know WHAT to pass in.
 */
@Component
public class WeatherTools {

    private final OpenMeteoClient openMeteo;

    public WeatherTools(OpenMeteoClient openMeteo) {
        this.openMeteo = openMeteo;
    }

    @McpTool(name = "get_current_weather",
            description = "Get the current weather (temperature, feels-like, humidity, wind, conditions) for a city.")
    public CurrentWeather getCurrentWeather(
            @McpToolParam(description = "City name, e.g. 'Dhaka' or 'London'") String city) {

        Location location = openMeteo.findCity(city);
        Current now = openMeteo.getForecast(location, 1).current();

        return new CurrentWeather(
                location.name() + ", " + location.country(),
                now.time(),
                now.temperature(),
                now.feelsLike(),
                now.humidity(),
                now.windSpeed(),
                WeatherCodes.describe(now.weatherCode()));
    }

    @McpTool(name = "get_forecast",
            description = "Get the daily weather forecast for a city: min/max temperature, conditions and chance of rain for each day.")
    public Forecast getForecast(
            @McpToolParam(description = "City name, e.g. 'Dhaka' or 'London'") String city,
            @McpToolParam(description = "Number of days to forecast, from 1 to 16. Defaults to 3.", required = false) Integer days) {

        // Use 3 days if not given, and keep the value inside what the API allows.
        int numberOfDays = (days == null) ? 3 : Math.clamp(days, 1, 16);

        Location location = openMeteo.findCity(city);
        Daily daily = openMeteo.getForecast(location, numberOfDays).daily();

        List<DayForecast> forecastDays = new ArrayList<>();
        for (int i = 0; i < daily.time().size(); i++) {
            forecastDays.add(new DayForecast(
                    daily.time().get(i),
                    WeatherCodes.describe(daily.weatherCode().get(i)),
                    daily.minTemperature().get(i),
                    daily.maxTemperature().get(i),
                    daily.rainChance().get(i)));
        }

        return new Forecast(location.name() + ", " + location.country(), forecastDays);
    }

    // ---- What the tools return. Spring AI sends these to the AI as JSON. ----

    public record CurrentWeather(String location, String time, double temperatureC, double feelsLikeC,
                                 int humidityPercent, double windSpeedKmh, String conditions) {}

    public record Forecast(String location, List<DayForecast> days) {}

    public record DayForecast(String date, String conditions, double minTemperatureC,
                              double maxTemperatureC, int rainChancePercent) {}
}

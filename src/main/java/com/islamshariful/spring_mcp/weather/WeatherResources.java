package com.islamshariful.spring_mcp.weather;

import com.islamshariful.spring_mcp.weather.WeatherTools.CurrentWeather;
import com.islamshariful.spring_mcp.weather.WeatherTools.DayForecast;
import com.islamshariful.spring_mcp.weather.WeatherTools.Forecast;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * MCP resources for weather.
 *
 * <p>Tool vs resource: a TOOL is something the model decides to call. A RESOURCE is context
 * that the user or app picks and attaches to the conversation, like attaching a file.
 * In Claude, resources show up in the "@" / attachment menu.
 */
@Component
public class WeatherResources {

    private final WeatherTools weather;

    public WeatherResources(WeatherTools weather) {
        this.weather = weather;
    }

    @McpResource(
            uri = "weather://{city}",
            name = "city-weather",
            title = "City weather",
            description = "Current weather and 3-day forecast for a city, e.g. weather://Dhaka",
            mimeType = "text/markdown")
    public String cityWeather(String city) {
        // City names with spaces arrive URL-encoded, e.g. "New%20York".
        String cityName = URLDecoder.decode(city, StandardCharsets.UTF_8);

        CurrentWeather now = weather.getCurrentWeather(cityName);
        Forecast forecast = weather.getForecast(cityName, 3);

        StringBuilder md = new StringBuilder();
        md.append("# Weather in ").append(now.location()).append("\n\n");
        md.append("## Now (").append(now.time()).append(")\n");
        md.append("- ").append(now.conditions()).append(", ").append(now.temperatureC()).append("°C")
                .append(" (feels like ").append(now.feelsLikeC()).append("°C)\n");
        md.append("- Humidity ").append(now.humidityPercent()).append("%, wind ")
                .append(now.windSpeedKmh()).append(" km/h\n\n");
        md.append("## Next 3 days\n");
        md.append("| Date | Conditions | Min °C | Max °C | Rain chance |\n");
        md.append("|---|---|---|---|---|\n");
        for (DayForecast day : forecast.days()) {
            md.append("| ").append(day.date())
                    .append(" | ").append(day.conditions())
                    .append(" | ").append(day.minTemperatureC())
                    .append(" | ").append(day.maxTemperatureC())
                    .append(" | ").append(day.rainChancePercent()).append("% |\n");
        }
        return md.toString();
    }
}

package com.islamshariful.spring_mcp.weather;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Talks to the free Open-Meteo APIs (no API key needed).
 * <ul>
 *   <li>Geocoding API: turns a city name into latitude/longitude.</li>
 *   <li>Forecast API: returns current weather and daily forecast for a location.</li>
 * </ul>
 */
@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private final RestClient geocodingApi = createClient("https://geocoding-api.open-meteo.com");
    private final RestClient forecastApi = createClient("https://api.open-meteo.com");

    /** Creates an HTTP client that gives up if the API doesn't answer in time, instead of hanging. */
    private static RestClient createClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /** Finds a city by name. Throws if nothing matches. */
    public Location findCity(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("Please provide a city name.");
        }

        GeocodingResponse response = call(() -> geocodingApi.get()
                .uri(uri -> uri.path("/v1/search")
                        .queryParam("name", city)
                        .queryParam("count", 1)
                        .build())
                .retrieve()
                .body(GeocodingResponse.class));

        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new IllegalArgumentException("City not found: " + city);
        }
        return response.results().getFirst();
    }

    /** Gets current weather plus a daily forecast for the given number of days. */
    public ForecastResponse getForecast(Location location, int days) {
        return call(() -> forecastApi.get()
                .uri(uri -> uri.path("/v1/forecast")
                        .queryParam("latitude", location.latitude())
                        .queryParam("longitude", location.longitude())
                        .queryParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                        .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                        .queryParam("timezone", "auto")
                        .queryParam("forecast_days", days)
                        .build())
                .retrieve()
                .body(ForecastResponse.class));
    }

    /**
     * Runs an API call. If the network fails (no internet, timeout, DNS problem),
     * it logs the full details for you and returns a clear message to the AI.
     */
    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            log.warn("Could not reach Open-Meteo", e);
            throw new IllegalStateException(
                    "Could not reach the weather service (Open-Meteo). Check the internet connection and try again.");
        }
    }

    // ---- JSON shapes returned by Open-Meteo (only the fields we use) ----

    public record GeocodingResponse(List<Location> results) {}

    public record Location(String name, String country, double latitude, double longitude, String timezone) {}

    public record ForecastResponse(Current current, Daily daily) {}

    public record Current(
            String time,
            @JsonProperty("temperature_2m") double temperature,
            @JsonProperty("apparent_temperature") double feelsLike,
            @JsonProperty("relative_humidity_2m") int humidity,
            @JsonProperty("wind_speed_10m") double windSpeed,
            @JsonProperty("weather_code") int weatherCode) {}

    public record Daily(
            List<String> time,
            @JsonProperty("weather_code") List<Integer> weatherCode,
            @JsonProperty("temperature_2m_max") List<Double> maxTemperature,
            @JsonProperty("temperature_2m_min") List<Double> minTemperature,
            @JsonProperty("precipitation_probability_max") List<Integer> rainChance) {}
}

package com.islamshariful.spring_mcp.weather;

/**
 * Open-Meteo returns weather as a number (WMO code).
 * This turns that number into words, e.g. 61 -> "Light rain".
 * Full list: https://open-meteo.com/en/docs (section "WMO Weather interpretation codes").
 */
final class WeatherCodes {

    private WeatherCodes() {}

    static String describe(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61 -> "Light rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown (code " + code + ")";
        };
    }
}

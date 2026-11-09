package com.islamshariful.spring_mcp.completions;

import com.islamshariful.spring_mcp.tasks.Task;
import com.islamshariful.spring_mcp.tasks.TaskService;
import com.islamshariful.spring_mcp.user.SecurityUserProvider.AuthenticationRequiredException;
import com.islamshariful.spring_mcp.weather.OpenMeteoClient;
import com.islamshariful.spring_mcp.weather.OpenMeteoClient.Location;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpComplete;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Autocomplete (MCP "completions") for prompt and resource arguments.
 * As the user types a value in the client (e.g. a city name), the client asks the
 * server for suggestions and shows them in a dropdown.
 *
 * <p>Each {@code @McpComplete} method is linked to one prompt or one resource URI template.
 */
@Component
public class ArgumentCompletions {

    private static final Logger log = LoggerFactory.getLogger(ArgumentCompletions.class);
    private static final int MAX_SUGGESTIONS = 10;

    /** Shown before the user types enough letters to search. */
    private static final List<String> POPULAR_CITIES = List.of(
            "Dhaka", "London", "New York", "Tokyo", "Paris", "Dubai",
            "Singapore", "Sydney", "Toronto", "Berlin", "Kuala Lumpur", "Istanbul");

    private static final List<String> TRIP_DAYS = List.of("1", "2", "3", "5", "7", "10", "14", "16");

    private final OpenMeteoClient openMeteo;
    private final TaskService tasks;

    public ArgumentCompletions(OpenMeteoClient openMeteo, TaskService tasks) {
        this.openMeteo = openMeteo;
        this.tasks = tasks;
    }

    // ---- Prompts ----

    @McpComplete(prompt = "plan_my_day")
    public List<String> completePlanMyDay(CompleteArgument argument) {
        return "city".equals(argument.name()) ? suggestCities(argument.value()) : List.of();
    }

    @McpComplete(prompt = "trip_prep")
    public List<String> completeTripPrep(CompleteArgument argument) {
        return switch (argument.name()) {
            case "city" -> suggestCities(argument.value());
            case "days" -> startingWith(TRIP_DAYS, argument.value());
            default -> List.of();
        };
    }

    // ---- Resources ----

    @McpComplete(uri = "weather://{city}")
    public List<String> completeWeatherCity(CompleteArgument argument) {
        return suggestCities(argument.value());
    }

    /** Suggests the current user's own task ids. Anonymous users get no suggestions. */
    @McpComplete(uri = "tasks://{id}")
    public List<String> completeTaskId(CompleteArgument argument) {
        try {
            return tasks.list(null, null, false).stream()
                    .map(Task::getId)
                    .map(String::valueOf)
                    .filter(id -> id.startsWith(nullToEmpty(argument.value())))
                    .limit(MAX_SUGGESTIONS)
                    .toList();
        } catch (AuthenticationRequiredException e) {
            return List.of();
        }
    }

    // ---- helpers ----

    /**
     * Live city search from Open-Meteo once the user has typed 2+ letters.
     * Before that, or if the API is unreachable, falls back to a list of popular cities.
     */
    private List<String> suggestCities(String typed) {
        String prefix = nullToEmpty(typed).strip();
        if (prefix.length() < 2) {
            return startingWith(POPULAR_CITIES, prefix);
        }
        try {
            return openMeteo.searchCities(prefix, MAX_SUGGESTIONS).stream()
                    .map(Location::name)
                    .distinct()
                    .toList();
        } catch (RuntimeException e) {
            log.debug("City search failed for '{}', using popular cities instead", prefix, e);
            return startingWith(POPULAR_CITIES, prefix);
        }
    }

    private static List<String> startingWith(List<String> values, String typed) {
        String prefix = nullToEmpty(typed).toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix))
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package com.islamshariful.spring_mcp.prompts;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP prompts: ready-made workflows the USER starts (in Claude: the "/" menu).
 *
 * <p>A prompt doesn't run anything itself. It fills in a well-written message that tells
 * the model which tools to use and what result to produce, so the user gets the same
 * good result every time without typing a long request.
 */
@Component
public class WorkflowPrompts {

    @McpPrompt(
            name = "plan_my_day",
            title = "Plan my day",
            description = "Plan today using the weather in your city and your open tasks.")
    public GetPromptResult planMyDay(
            @McpArg(name = "city", description = "Your city, e.g. Dhaka", required = true) String city) {

        String text = """
                Help me plan my day. Today is %s and I'm in %s.

                1. Use get_forecast for %s (1 day) to check today's weather.
                2. Use task_summary, then list_tasks with overdueOnly=true and list_tasks with status TODO and IN_PROGRESS.
                3. Give me a short plan:
                   - A one-line weather summary and what it means for me (umbrella, outdoor tasks, etc.).
                   - Overdue tasks first, then tasks due today, then HIGH priority ones.
                   - Suggest a realistic order for the day with at most 5 tasks.
                4. Don't change any tasks. Just suggest. Ask me before marking anything done.
                """.formatted(LocalDate.now(), city, city);

        return userPrompt("Daily plan for " + city, text);
    }

    @McpPrompt(
            name = "weekly_review",
            title = "Weekly review",
            description = "Review your tasks: what's done, what's overdue, and what to focus on next week.")
    public GetPromptResult weeklyReview() {
        String text = """
                Run my weekly task review. Today is %s.

                1. Use task_summary to get the overall numbers.
                2. Use list_tasks to see all tasks, then group them into:
                   - ✅ Done
                   - ⚠️ Overdue (most urgent first)
                   - 📅 Due in the next 7 days
                   - 💤 No due date
                3. Point out tasks that look stuck (IN_PROGRESS with an old or missing due date).
                4. Suggest my top 3 focus tasks for next week and explain why in one line each.
                5. Suggest tasks that could be deleted or rescheduled, but don't change anything
                   until I confirm.
                """.formatted(LocalDate.now());

        return userPrompt("Weekly task review", text);
    }

    @McpPrompt(
            name = "trip_prep",
            title = "Prepare for a trip",
            description = "Check the forecast for your destination, get a packing list, and add prep tasks.")
    public GetPromptResult tripPrep(
            @McpArg(name = "city", description = "Destination city, e.g. London", required = true) String city,
            @McpArg(name = "days", description = "Length of the trip in days (1-16). Defaults to 3.", required = false) String days) {

        int tripDays = parseDays(days);
        String text = """
                I'm traveling to %s for %d days, starting today (%s).

                1. Use get_forecast for %s with days=%d.
                2. Summarize the weather for the trip in 2-3 lines (temperatures, rainy days).
                3. Give me a packing list that fits this weather, grouped into Clothes, Gear, and Other.
                4. Suggest up to 3 preparation tasks (e.g. "Buy travel adapter") with sensible due dates
                   before the trip. Ask me which ones to add, then use create_task for the ones I pick.
                """.formatted(city, tripDays, LocalDate.now(), city, tripDays);

        return userPrompt("Trip prep for " + city, text);
    }

    // ---- helpers ----

    private static GetPromptResult userPrompt(String description, String text) {
        return new GetPromptResult(description, List.of(new PromptMessage(Role.USER, new TextContent(text))));
    }

    /** Prompt arguments always arrive as text, so convert and keep within the forecast limit. */
    private static int parseDays(String days) {
        if (days == null || days.isBlank()) return 3;
        try {
            return Math.clamp(Integer.parseInt(days.strip()), 1, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("days must be a number from 1 to 16, e.g. 5");
        }
    }
}

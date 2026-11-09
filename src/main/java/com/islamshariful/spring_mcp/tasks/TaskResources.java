package com.islamshariful.spring_mcp.tasks;

import com.islamshariful.spring_mcp.tasks.Task.Status;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP resources for tasks. Like the task tools, these need an API key
 * and only show the current user's tasks (they go through {@link TaskService}).
 */
@Component
public class TaskResources {

    private final TaskService tasks;

    public TaskResources(TaskService tasks) {
        this.tasks = tasks;
    }

    @McpResource(
            uri = "tasks://open",
            name = "open-tasks",
            title = "My open tasks",
            description = "All of your tasks that aren't done yet, sorted by due date. Needs an API key.",
            mimeType = "text/markdown")
    public String openTasks() {
        LocalDate today = tasks.today();
        List<Task> open = tasks.list(null, null, false).stream()
                .filter(task -> task.getStatus() != Status.DONE)
                .toList();

        if (open.isEmpty()) {
            return "# Open tasks\n\nNo open tasks. 🎉\n";
        }

        StringBuilder md = new StringBuilder("# Open tasks (" + open.size() + ")\n\n");
        md.append("| Id | Title | Status | Priority | Due |\n");
        md.append("|---|---|---|---|---|\n");
        for (Task task : open) {
            md.append("| ").append(task.getId())
                    .append(" | ").append(task.getTitle())
                    .append(" | ").append(task.getStatus())
                    .append(" | ").append(task.getPriority())
                    .append(" | ").append(dueText(task, today)).append(" |\n");
        }
        return md.toString();
    }

    @McpResource(
            uri = "tasks://{id}",
            name = "task",
            title = "Task details",
            description = "Full details of one of your tasks, e.g. tasks://1. Needs an API key.",
            mimeType = "text/markdown")
    public String task(String id) {
        Task task = tasks.get(parseId(id));
        LocalDate today = tasks.today();

        return """
                # Task %d: %s

                - **Status:** %s
                - **Priority:** %s
                - **Due:** %s
                - **Created:** %s
                - **Updated:** %s

                %s
                """.formatted(
                task.getId(), task.getTitle(),
                task.getStatus(), task.getPriority(), dueText(task, today),
                task.getCreatedAt(), task.getUpdatedAt(),
                task.getDescription() == null ? "_No description._" : task.getDescription());
    }

    private static String dueText(Task task, LocalDate today) {
        if (task.getDueDate() == null) return "—";
        return task.isOverdue(today) ? task.getDueDate() + " ⚠️ overdue" : task.getDueDate().toString();
    }

    private static Long parseId(String id) {
        try {
            return Long.parseLong(id.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Task id must be a number, e.g. tasks://1");
        }
    }
}

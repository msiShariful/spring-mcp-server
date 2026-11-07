package com.islamshariful.spring_mcp.tasks;

import com.islamshariful.spring_mcp.tasks.Task.Priority;
import com.islamshariful.spring_mcp.tasks.Task.Status;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * MCP tools for a personal task manager.
 *
 * <p>These tools read and change the user's own data, so they are the ones
 * to put behind authentication. Ownership is already handled: every call
 * goes through {@link TaskService}, which only touches the current user's tasks.
 *
 * <p>The {@code annotations} tell AI clients how risky each tool is:
 * readOnlyHint = only reads data, destructiveHint = can delete data.
 * Clients like Claude use these to decide when to ask the user for confirmation.
 */
@Component
public class TaskTools {

    private final TaskService tasks;

    public TaskTools(TaskService tasks) {
        this.tasks = tasks;
    }

    @McpTool(name = "create_task",
            description = "Create a new task in the user's to-do list. Returns the created task with its id.",
            annotations = @McpAnnotations(title = "Create task", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = false, openWorldHint = false))
    public TaskView createTask(
            @McpToolParam(description = "Short title, e.g. 'Renew passport'") String title,
            @McpToolParam(description = "Optional details about the task", required = false) String description,
            @McpToolParam(description = "LOW, MEDIUM or HIGH. Defaults to MEDIUM.", required = false) Priority priority,
            @McpToolParam(description = "Optional due date in YYYY-MM-DD format", required = false) String dueDate) {

        return TaskView.from(tasks.create(title, description, priority, dueDate), tasks.today());
    }

    @McpTool(name = "list_tasks",
            description = "List the user's tasks, sorted by due date. Optionally filter by status, priority, or only overdue tasks.",
            annotations = @McpAnnotations(title = "List tasks", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<TaskView> listTasks(
            @McpToolParam(description = "Only tasks with this status: TODO, IN_PROGRESS or DONE", required = false) Status status,
            @McpToolParam(description = "Only tasks with this priority: LOW, MEDIUM or HIGH", required = false) Priority priority,
            @McpToolParam(description = "If true, only return tasks that are past their due date and not done", required = false) Boolean overdueOnly) {

        LocalDate today = tasks.today();
        return tasks.list(status, priority, Boolean.TRUE.equals(overdueOnly)).stream()
                .map(task -> TaskView.from(task, today))
                .toList();
    }

    @McpTool(name = "get_task",
            description = "Get the full details of one task by its id.",
            annotations = @McpAnnotations(title = "Get task", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public TaskView getTask(@McpToolParam(description = "The task id") Long taskId) {
        return TaskView.from(tasks.get(taskId), tasks.today());
    }

    @McpTool(name = "update_task",
            description = "Update a task. Only the fields you pass are changed; leave the others empty. "
                    + "Use dueDate 'none' to remove the due date.",
            annotations = @McpAnnotations(title = "Update task", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public TaskView updateTask(
            @McpToolParam(description = "The task id") Long taskId,
            @McpToolParam(description = "New title", required = false) String title,
            @McpToolParam(description = "New description", required = false) String description,
            @McpToolParam(description = "New status: TODO, IN_PROGRESS or DONE", required = false) Status status,
            @McpToolParam(description = "New priority: LOW, MEDIUM or HIGH", required = false) Priority priority,
            @McpToolParam(description = "New due date in YYYY-MM-DD format, or 'none' to remove it", required = false) String dueDate) {

        return TaskView.from(tasks.update(taskId, title, description, status, priority, dueDate), tasks.today());
    }

    @McpTool(name = "complete_task",
            description = "Mark a task as done.",
            annotations = @McpAnnotations(title = "Complete task", readOnlyHint = false, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public TaskView completeTask(@McpToolParam(description = "The task id") Long taskId) {
        return TaskView.from(tasks.complete(taskId), tasks.today());
    }

    @McpTool(name = "delete_task",
            description = "Permanently delete a task. This cannot be undone, so confirm with the user first.",
            annotations = @McpAnnotations(title = "Delete task", readOnlyHint = false, destructiveHint = true,
                    idempotentHint = false, openWorldHint = false))
    public String deleteTask(@McpToolParam(description = "The task id") Long taskId) {
        Task deleted = tasks.delete(taskId);
        return "Deleted task " + deleted.getId() + ": " + deleted.getTitle();
    }

    @McpTool(name = "task_summary",
            description = "Get a quick overview of the user's tasks: totals by status, how many are overdue and due today.",
            annotations = @McpAnnotations(title = "Task summary", readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public TaskService.Summary taskSummary() {
        return tasks.summary();
    }

    // ---- What the tools return. Spring AI sends this to the AI as JSON. ----

    /** A task as shown to the AI. The owner isn't included; it's always the current user. */
    public record TaskView(Long id, String title, String description, Status status, Priority priority,
                           LocalDate dueDate, boolean overdue) {

        static TaskView from(Task task, LocalDate today) {
            return new TaskView(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                    task.getPriority(), task.getDueDate(), task.isOverdue(today));
        }
    }
}

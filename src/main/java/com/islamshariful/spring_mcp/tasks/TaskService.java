package com.islamshariful.spring_mcp.tasks;

import com.islamshariful.spring_mcp.tasks.Task.Priority;
import com.islamshariful.spring_mcp.tasks.Task.Status;
import com.islamshariful.spring_mcp.user.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Business logic for tasks. Every method works on the CURRENT user's tasks only.
 * The MCP tools call this class; a REST controller could reuse it too.
 */
@Service
@Transactional
public class TaskService {

    private final TaskRepository repository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    @Autowired
    public TaskService(TaskRepository repository, CurrentUserProvider currentUser) {
        this(repository, currentUser, Clock.systemDefaultZone());
    }

    /** Used by tests to act as a specific user on a fixed date. */
    TaskService(TaskRepository repository, CurrentUserProvider currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public Task create(String title, String description, Priority priority, String dueDate) {
        Task task = new Task(
                currentUser.currentUserId(),
                requireTitle(title),
                blankToNull(description),
                priority == null ? Priority.MEDIUM : priority,
                parseDate(dueDate));
        return repository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> list(Status status, Priority priority, boolean overdueOnly) {
        LocalDate today = today();
        return repository.findByOwnerOrderByDueDateAscIdAsc(currentUser.currentUserId()).stream()
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .filter(task -> !overdueOnly || task.isOverdue(today))
                .toList();
    }

    @Transactional(readOnly = true)
    public Task get(Long id) {
        return findOwned(id);
    }

    /** Changes only the fields that are given (non-null, non-blank). */
    public Task update(Long id, String title, String description, Status status, Priority priority, String dueDate) {
        Task task = findOwned(id);
        if (title != null && !title.isBlank()) task.setTitle(requireTitle(title));
        if (description != null && !description.isBlank()) task.setDescription(description.strip());
        if (status != null) task.setStatus(status);
        if (priority != null) task.setPriority(priority);
        if (dueDate != null && !dueDate.isBlank()) {
            task.setDueDate("none".equalsIgnoreCase(dueDate.strip()) ? null : parseDate(dueDate));
        }
        return repository.save(task);
    }

    public Task complete(Long id) {
        Task task = findOwned(id);
        task.setStatus(Status.DONE);
        return repository.save(task);
    }

    public Task delete(Long id) {
        Task task = findOwned(id);
        repository.delete(task);
        return task;
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        LocalDate today = today();
        List<Task> tasks = repository.findByOwnerOrderByDueDateAscIdAsc(currentUser.currentUserId());
        return new Summary(
                tasks.size(),
                count(tasks, Status.TODO),
                count(tasks, Status.IN_PROGRESS),
                count(tasks, Status.DONE),
                tasks.stream().filter(t -> t.isOverdue(today)).count(),
                tasks.stream().filter(t -> today.equals(t.getDueDate()) && t.getStatus() != Status.DONE).count());
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public record Summary(long total, long todo, long inProgress, long done, long overdue, long dueToday) {}

    // ---- helpers ----

    /** Looks up a task by id, but only if it belongs to the current user. */
    private Task findOwned(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Please provide a task id.");
        }
        // Same message whether the task doesn't exist or belongs to someone else,
        // so other users' task ids aren't revealed.
        return repository.findByIdAndOwner(id, currentUser.currentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Task title can't be empty.");
        }
        String trimmed = title.strip();
        if (trimmed.length() > 200) {
            throw new IllegalArgumentException("Task title is too long (max 200 characters).");
        }
        return trimmed;
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.strip());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date '" + date + "'. Use the format YYYY-MM-DD, e.g. 2026-12-31.");
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }

    private static long count(List<Task> tasks, Status status) {
        return tasks.stream().filter(t -> t.getStatus() == status).count();
    }
}

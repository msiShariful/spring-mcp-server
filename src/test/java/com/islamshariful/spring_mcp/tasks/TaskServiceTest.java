package com.islamshariful.spring_mcp.tasks;

import com.islamshariful.spring_mcp.tasks.Task.Priority;
import com.islamshariful.spring_mcp.tasks.Task.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:task-test")
class TaskServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private TaskRepository repository;

    /** Two services acting as two different users, sharing one database. */
    private TaskService alice;
    private TaskService bob;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        alice = new TaskService(repository, () -> "alice", FIXED_CLOCK);
        bob = new TaskService(repository, () -> "bob", FIXED_CLOCK);
    }

    @Test
    void createsTaskWithDefaults() {
        Task task = alice.create("  Renew passport  ", null, null, null);

        assertThat(task.getId()).isNotNull();
        assertThat(task.getTitle()).isEqualTo("Renew passport");
        assertThat(task.getStatus()).isEqualTo(Status.TODO);
        assertThat(task.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(task.getOwner()).isEqualTo("alice");
    }

    @Test
    void usersOnlySeeTheirOwnTasks() {
        Task aliceTask = alice.create("Alice's task", null, null, null);
        bob.create("Bob's task", null, null, null);

        assertThat(alice.list(null, null, false)).extracting(Task::getTitle).containsExactly("Alice's task");
        assertThat(bob.list(null, null, false)).extracting(Task::getTitle).containsExactly("Bob's task");

        // Bob can't read, change or delete Alice's task, even with the right id.
        Long id = aliceTask.getId();
        assertThatThrownBy(() -> bob.get(id)).hasMessage("Task not found: " + id);
        assertThatThrownBy(() -> bob.complete(id)).hasMessage("Task not found: " + id);
        assertThatThrownBy(() -> bob.delete(id)).hasMessage("Task not found: " + id);
        assertThat(alice.get(id).getStatus()).isEqualTo(Status.TODO);
    }

    @Test
    void filtersAndSummarizes() {
        alice.create("Overdue", null, Priority.HIGH, "2026-11-01");
        alice.create("Due today", null, Priority.LOW, "2026-11-07");
        Task done = alice.create("Already done", null, null, "2026-10-01");
        alice.complete(done.getId());

        assertThat(alice.list(null, null, true)).extracting(Task::getTitle).containsExactly("Overdue");
        assertThat(alice.list(null, Priority.HIGH, false)).extracting(Task::getTitle).containsExactly("Overdue");
        assertThat(alice.list(Status.DONE, null, false)).extracting(Task::getTitle).containsExactly("Already done");

        assertThat(alice.summary()).isEqualTo(new TaskService.Summary(3, 2, 0, 1, 1, 1));
    }

    @Test
    void updatesOnlyGivenFields() {
        Task task = alice.create("Write report", "Q4 numbers", Priority.LOW, "2026-11-20");

        alice.update(task.getId(), null, null, Status.IN_PROGRESS, null, "none");

        Task updated = alice.get(task.getId());
        assertThat(updated.getTitle()).isEqualTo("Write report");
        assertThat(updated.getDescription()).isEqualTo("Q4 numbers");
        assertThat(updated.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(updated.getDueDate()).isNull();
    }

    @Test
    void rejectsBadInput() {
        assertThatThrownBy(() -> alice.create(" ", null, null, null))
                .hasMessage("Task title can't be empty.");
        assertThatThrownBy(() -> alice.create("Task", null, null, "31-12-2026"))
                .hasMessageContaining("Use the format YYYY-MM-DD");
        assertThat(alice.create("Task", null, null, "2026-12-31").getDueDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    }
}

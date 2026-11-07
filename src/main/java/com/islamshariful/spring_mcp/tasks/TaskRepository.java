package com.islamshariful.spring_mcp.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Database access for tasks. Every method takes the owner,
 * so one user can never read or change another user's tasks.
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByOwnerOrderByDueDateAscIdAsc(String owner);

    Optional<Task> findByIdAndOwner(Long id, String owner);
}

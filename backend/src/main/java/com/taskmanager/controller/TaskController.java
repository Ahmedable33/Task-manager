package com.taskmanager.controller;

import com.taskmanager.model.*;
import com.taskmanager.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskRepository tasks; private final UserRepository users;
  public TaskController(TaskRepository tasks, UserRepository users) { this.tasks = tasks; this.users = users; }
  public record TaskRequest(@NotBlank String title, String description, TaskStatus status) {}
  @GetMapping public List<Task> all(Authentication auth) { return tasks.findAllByUserIdOrderByUpdatedAtDesc(currentUser(auth).getId()); }
  @PostMapping public ResponseEntity<Task> create(@Valid @RequestBody TaskRequest body, Authentication auth) { Task task = new Task(); apply(task, body); task.setUser(currentUser(auth)); return ResponseEntity.status(HttpStatus.CREATED).body(tasks.save(task)); }
  @PutMapping("/{id}") public ResponseEntity<Task> update(@PathVariable Long id, @Valid @RequestBody TaskRequest body, Authentication auth) { return tasks.findByIdAndUserId(id, currentUser(auth).getId()).map(task -> { apply(task, body); return ResponseEntity.ok(tasks.save(task)); }).orElseGet(() -> ResponseEntity.notFound().build()); }
  @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) { return tasks.findByIdAndUserId(id, currentUser(auth).getId()).map(task -> { tasks.delete(task); return ResponseEntity.noContent().<Void>build(); }).orElseGet(() -> ResponseEntity.notFound().build()); }
  private User currentUser(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(); }
  private void apply(Task task, TaskRequest body) { task.setTitle(body.title()); task.setDescription(body.description()); task.setStatus(body.status() == null ? TaskStatus.TODO : body.status()); }
}

package com.taskmanager.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {
  @Mock private TaskRepository tasks;
  @Mock private UserRepository users;
  @Mock private Authentication authentication;
  @Mock private User user;

  private TaskController controller;

  @BeforeEach
  void setUp() {
    controller = new TaskController(tasks, users);
    when(authentication.getName()).thenReturn("user@example.com");
    when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
  }

  @Test
  void createsTodoTaskWhenStatusIsMissing() {
    when(tasks.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ResponseEntity<Task> response = controller.create(
        new TaskController.TaskRequest("Prepare release", "Run checks", null), authentication);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Prepare release", response.getBody().getTitle());
    assertEquals("Run checks", response.getBody().getDescription());
    assertEquals(TaskStatus.TODO, response.getBody().getStatus());
  }

  @Test
  void updatesTaskOwnedByCurrentUser() {
    Task task = new Task();
    when(user.getId()).thenReturn(7L);
    when(tasks.findByIdAndUserId(12L, 7L)).thenReturn(Optional.of(task));
    when(tasks.save(task)).thenReturn(task);

    ResponseEntity<Task> response = controller.update(
        12L,
        new TaskController.TaskRequest("Updated title", "Updated description", TaskStatus.DONE),
        authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("Updated title", task.getTitle());
    assertEquals(TaskStatus.DONE, task.getStatus());
    verify(tasks).save(task);
  }

  @Test
  void returnsNotFoundForTaskNotOwnedByCurrentUser() {
    when(user.getId()).thenReturn(7L);
    when(tasks.findByIdAndUserId(12L, 7L)).thenReturn(Optional.empty());

    ResponseEntity<Task> response = controller.update(
        12L,
        new TaskController.TaskRequest("Updated title", null, TaskStatus.DONE),
        authentication);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }
}

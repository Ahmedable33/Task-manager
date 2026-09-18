package com.taskmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String title;
  @Column(columnDefinition = "TEXT")
  private String description;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status = TaskStatus.TODO;
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;
  @Column(nullable = false)
  private LocalDateTime updatedAt;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
  @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
  public Long getId() { return id; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public TaskStatus getStatus() { return status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setTitle(String title) { this.title = title; }
  public void setDescription(String description) { this.description = description; }
  public void setStatus(TaskStatus status) { this.status = status; }
  public void setUser(User user) { this.user = user; }
}

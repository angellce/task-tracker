package com.example.project1.dto;

import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class TaskResponseDto {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private String assignee;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;

    public TaskResponseDto(Long id, String title, String description, TaskStatus status, TaskPriority priority, LocalDate dueDate, String assignee, Long projectId, String projectName, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.assignee = assignee;
        this.projectId = projectId;
        this.projectName = projectName;
        this.createdAt = createdAt;
    }
}

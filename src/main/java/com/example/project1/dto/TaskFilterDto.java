package com.example.project1.dto;

import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import lombok.Getter;

import java.time.LocalDate;

// Optional filters for GET /api/tasks; a null field means "do not filter on this"
@Getter
public class TaskFilterDto {
    private TaskStatus status;
    private TaskPriority priority;
    private Long projectId;
    private String assignee;
    private LocalDate dueBefore;

    public TaskFilterDto(TaskStatus status, TaskPriority priority, Long projectId, String assignee, LocalDate dueBefore) {
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assignee = assignee;
        this.dueBefore = dueBefore;
    }
}

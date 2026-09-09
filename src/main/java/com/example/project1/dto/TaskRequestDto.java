package com.example.project1.dto;

import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequestDto {
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private String assignee;
    private Long projectId;
}

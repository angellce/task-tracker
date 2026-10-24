package com.example.project1.dto;

import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskRequestDto {
    // limits match the V1 migration columns (varchar(255))
    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String description;

    private TaskStatus status;
    private TaskPriority priority;

    @Future
    private LocalDate dueDate;

    @Size(max = 255)
    private String assignee;

    private Long projectId;
}

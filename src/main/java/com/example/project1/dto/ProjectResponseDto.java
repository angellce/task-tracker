package com.example.project1.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter

public class ProjectResponseDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    public ProjectResponseDto(Long id, String name, String description, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }
}

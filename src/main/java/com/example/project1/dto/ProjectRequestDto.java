package com.example.project1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequestDto {
    // limits match the V1 migration columns (varchar(255))
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String description;
}

package com.example.project1.controller;

import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponseDto create(@Valid @RequestBody ProjectRequestDto request){
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectResponseDto> findAll(){
        return projectService.findAll();
    }

    @GetMapping("{id}")
    public ProjectResponseDto findById(@PathVariable Long id){
        return projectService.findById(id);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id){
        projectService.delete(id);
    }

    @PutMapping("{id}")
    public ProjectResponseDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequestDto projectRequestDto){
        return projectService.update(id, projectRequestDto);
    }

}

package com.example.project1.controller;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public PageResponseDto<ProjectResponseDto> findAll(@PageableDefault(size = 20, sort = "id") Pageable pageable){
        return projectService.findAll(pageable);
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

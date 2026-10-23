package com.example.project1.controller;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.TaskRequestDto;
import com.example.project1.dto.TaskResponseDto;
import com.example.project1.model.Task;
import com.example.project1.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public TaskResponseDto create(@PathVariable Long projectId, @Valid @RequestBody TaskRequestDto request){
        request.setProjectId(projectId);
        return taskService.create(request);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public PageResponseDto<TaskResponseDto> findByProject(@PathVariable Long projectId,
                                                          @PageableDefault(size = 20, sort = "id") Pageable pageable){
        return taskService.findByProject(projectId, pageable);
    }

    @GetMapping("/api/tasks/{id}")
    public TaskResponseDto findById(@PathVariable Long id){
        return taskService.findById(id);
    }

    @PutMapping("/api/tasks/{id}")
    public TaskResponseDto update(@PathVariable Long id, @Valid @RequestBody TaskRequestDto request){
        return taskService.update(id, request);
    }

    @DeleteMapping("/api/tasks/{id}")
    public void delete(@PathVariable Long id){
        taskService.delete(id);
    }
}

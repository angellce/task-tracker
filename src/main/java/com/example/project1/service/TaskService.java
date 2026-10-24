package com.example.project1.service;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.TaskFilterDto;
import com.example.project1.dto.TaskRequestDto;
import com.example.project1.dto.TaskResponseDto;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Project;
import com.example.project1.model.Task;
import com.example.project1.repository.ProjectRepository;
import com.example.project1.repository.TaskRepository;
import com.example.project1.repository.TaskSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    public TaskResponseDto create(TaskRequestDto request) {
        Project project = getProjectOrThrow(request.getProjectId());

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());
        task.setDueDate(request.getDueDate());
        task.setAssignee(request.getAssignee());
        task.setProject(project);

        Task saved = taskRepository.save(task);
        return toResponseDto(saved);
    }

    public PageResponseDto<TaskResponseDto> findByProject(Long projectId, Pageable pageable) {
        return new PageResponseDto<>(taskRepository.findByProjectId(projectId, pageable).map(this::toResponseDto));
    }

    public PageResponseDto<TaskResponseDto> findAll(TaskFilterDto filter, Pageable pageable) {
        return new PageResponseDto<>(
                taskRepository.findAll(TaskSpecifications.fromFilter(filter), pageable).map(this::toResponseDto));
    }

    public TaskResponseDto findById(Long id) {
        return toResponseDto(getTaskOrThrow(id));
    }

    public TaskResponseDto update(Long id, TaskRequestDto request) {
        Task existing = getTaskOrThrow(id);
        Project project = getProjectOrThrow(request.getProjectId());

        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setStatus(request.getStatus());
        existing.setPriority(request.getPriority());
        existing.setDueDate(request.getDueDate());
        existing.setAssignee(request.getAssignee());
        existing.setProject(project);

        Task saved = taskRepository.save(existing);
        return toResponseDto(saved);
    }

    public void delete(Long id) {
        Task existing = getTaskOrThrow(id);
        taskRepository.delete(existing);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private TaskResponseDto toResponseDto(Task task) {
        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getAssignee(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getCreatedAt()
        );
    }
}


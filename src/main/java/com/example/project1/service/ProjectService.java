package com.example.project1.service;

import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Project;
import com.example.project1.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponseDto create(ProjectRequestDto request) {
        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project saved = projectRepository.save(project);

        return new ProjectResponseDto(saved.getId(), saved.getName(), saved.getDescription(), saved.getCreatedAt());
    }

    public List<ProjectResponseDto> findAll(){
        return projectRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public ProjectResponseDto findById(Long id){
        Project project = getProjectOrThrow(id);
        return toResponseDto(project);

    }

    public ProjectResponseDto update(Long id, ProjectRequestDto request) {
        Project existing = getProjectOrThrow(id);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());

        Project saved = projectRepository.save(existing);
        return toResponseDto(saved);
    }

    public void delete(Long id) {
        Project existing = getProjectOrThrow(id);
        projectRepository.delete(existing);
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
    }

    private ProjectResponseDto toResponseDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }
}

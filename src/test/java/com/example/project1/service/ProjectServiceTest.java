package com.example.project1.service;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Project;
import com.example.project1.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    // @PrePersist does not run with a mocked repository, so tests set createdAt themselves
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 15, 10, 30);

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void create_validRequest_savesProjectAndReturnsDto() {
        ProjectRequestDto request = request("Website", "Company site");
        Project saved = project(1L, "Website", "Company site");
        when(projectRepository.save(any(Project.class))).thenReturn(saved);

        ProjectResponseDto result = projectService.create(request);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project toSave = captor.getValue();
        assertThat(toSave.getId()).isNull();
        assertThat(toSave.getName()).isEqualTo("Website");
        assertThat(toSave.getDescription()).isEqualTo("Company site");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Website");
        assertThat(result.getDescription()).isEqualTo("Company site");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void findAll_projectsExist_returnsMappedPage() {
        // page 1 of size 2, with 5 projects in total
        Pageable pageable = PageRequest.of(1, 2, Sort.by("name"));
        Page<Project> page = new PageImpl<>(List.of(
                project(3L, "Website", "Company site"),
                project(4L, "Mobile app", null)
        ), pageable, 5);
        when(projectRepository.findAll(pageable)).thenReturn(page);

        PageResponseDto<ProjectResponseDto> result = projectService.findAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Website");
        assertThat(result.getContent().get(1).getId()).isEqualTo(4L);
        assertThat(result.getContent().get(1).getDescription()).isNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    @Test
    void findAll_noProjects_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(projectRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        PageResponseDto<ProjectResponseDto> result = projectService.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getTotalPages()).isZero();
    }

    @Test
    void findById_existingId_returnsDto() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project(1L, "Website", "Company site")));

        ProjectResponseDto result = projectService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Website");
        assertThat(result.getDescription()).isEqualTo("Company site");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void findById_missingId_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
    }

    @Test
    void update_existingId_updatesFieldsAndReturnsDto() {
        Project existing = project(1L, "Old name", "Old description");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponseDto result = projectService.update(1L, request("New name", "New description"));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project toSave = captor.getValue();
        assertThat(toSave).isSameAs(existing);
        assertThat(toSave.getId()).isEqualTo(1L);
        assertThat(toSave.getName()).isEqualTo("New name");
        assertThat(toSave.getDescription()).isEqualTo("New description");
        assertThat(toSave.getCreatedAt()).isEqualTo(CREATED_AT);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("New name");
        assertThat(result.getDescription()).isEqualTo("New description");
    }

    @Test
    void update_missingId_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(99L, request("New name", "New description")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
        verify(projectRepository, never()).save(any());
    }

    @Test
    void delete_existingId_deletesProject() {
        Project existing = project(1L, "Website", "Company site");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));

        projectService.delete(1L);

        verify(projectRepository).delete(existing);
    }

    @Test
    void delete_missingId_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
        verify(projectRepository, never()).delete(any());
    }

    private ProjectRequestDto request(String name, String description) {
        ProjectRequestDto request = new ProjectRequestDto();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private Project project(Long id, String name, String description) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription(description);
        project.setCreatedAt(CREATED_AT);
        return project;
    }
}

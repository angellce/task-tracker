package com.example.project1.service;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.TaskRequestDto;
import com.example.project1.dto.TaskResponseDto;
import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Project;
import com.example.project1.model.Task;
import com.example.project1.repository.ProjectRepository;
import com.example.project1.repository.TaskRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    // @PrePersist does not run with a mocked repository, so tests set createdAt themselves
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final LocalDate OLD_DUE_DATE = LocalDate.of(2026, 2, 1);
    private static final LocalDate NEW_DUE_DATE = LocalDate.of(2026, 3, 1);

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_projectExists_savesTaskAndReturnsDto() {
        Project project = project(1L, "Website");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task toSave = invocation.getArgument(0);
            toSave.setId(10L);
            toSave.setCreatedAt(CREATED_AT);
            return toSave;
        });

        TaskResponseDto result = taskService.create(request(1L));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("New title");
        assertThat(saved.getDescription()).isEqualTo("New description");
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(saved.getDueDate()).isEqualTo(NEW_DUE_DATE);
        assertThat(saved.getAssignee()).isEqualTo("bob");
        assertThat(saved.getProject()).isSameAs(project);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getProjectName()).isEqualTo("Website");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void create_projectNotFound_throwsResourceNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.create(request(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
        verifyNoInteractions(taskRepository);
    }

    @Test
    void findById_existingId_returnsDtoWithAllFieldsMapped() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L, project(1L, "Website"))));

        TaskResponseDto result = taskService.findById(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getTitle()).isEqualTo("Old title");
        assertThat(result.getDescription()).isEqualTo("Old description");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(result.getDueDate()).isEqualTo(OLD_DUE_DATE);
        assertThat(result.getAssignee()).isEqualTo("alice");
        assertThat(result.getProjectId()).isEqualTo(1L);
        assertThat(result.getProjectName()).isEqualTo("Website");
        assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void findById_missingId_throwsResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
    }

    @Test
    void update_existingTask_updatesFieldsAndReturnsDto() {
        Project project = project(1L, "Website");
        Task existing = task(10L, project);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDto result = taskService.update(10L, request(1L));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(saved.getTitle()).isEqualTo("New title");
        assertThat(saved.getDescription()).isEqualTo("New description");
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(saved.getDueDate()).isEqualTo(NEW_DUE_DATE);
        assertThat(saved.getAssignee()).isEqualTo("bob");

        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void update_newProjectId_movesTaskToNewProject() {
        Project oldProject = project(1L, "Website");
        Project newProject = project(2L, "Mobile app");
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L, oldProject)));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(newProject));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponseDto result = taskService.update(10L, request(2L));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getProject()).isSameAs(newProject);
        assertThat(result.getProjectId()).isEqualTo(2L);
        assertThat(result.getProjectName()).isEqualTo("Mobile app");
    }

    @Test
    void update_missingTask_throwsResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, request(1L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void update_projectNotFound_throwsResourceNotFoundException() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task(10L, project(1L, "Website"))));
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(10L, request(99L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id: 99");
        verify(taskRepository, never()).save(any());
    }

    @Test
    void delete_existingId_deletesTask() {
        Task existing = task(10L, project(1L, "Website"));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(existing));

        taskService.delete(10L);

        verify(taskRepository).delete(existing);
    }

    @Test
    void delete_missingId_throwsResourceNotFoundException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found with id: 99");
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void findByProject_tasksExist_returnsMappedPage() {
        Project project = project(1L, "Website");
        Pageable pageable = PageRequest.of(0, 2, Sort.by("dueDate"));
        Page<Task> page = new PageImpl<>(List.of(task(10L, project), task(11L, project)), pageable, 3);
        when(taskRepository.findByProjectId(1L, pageable)).thenReturn(page);

        PageResponseDto<TaskResponseDto> result = taskService.findByProject(1L, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getId()).isEqualTo(10L);
        assertThat(result.getContent().get(1).getId()).isEqualTo(11L);
        assertThat(result.getContent()).allSatisfy(dto -> assertThat(dto.getProjectId()).isEqualTo(1L));
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findByProject_noTasks_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(taskRepository.findByProjectId(1L, pageable)).thenReturn(Page.empty(pageable));

        PageResponseDto<TaskResponseDto> result = taskService.findByProject(1L, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // request values differ from task(...) values, so a field the service forgets to copy is caught
    private TaskRequestDto request(Long projectId) {
        TaskRequestDto request = new TaskRequestDto();
        request.setTitle("New title");
        request.setDescription("New description");
        request.setStatus(TaskStatus.COMPLETED);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(NEW_DUE_DATE);
        request.setAssignee("bob");
        request.setProjectId(projectId);
        return request;
    }

    private Task task(Long id, Project project) {
        Task task = new Task();
        task.setId(id);
        task.setTitle("Old title");
        task.setDescription("Old description");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.LOW);
        task.setDueDate(OLD_DUE_DATE);
        task.setAssignee("alice");
        task.setProject(project);
        task.setCreatedAt(CREATED_AT);
        return task;
    }

    private Project project(Long id, String name) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setCreatedAt(CREATED_AT);
        return project;
    }
}

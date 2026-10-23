package com.example.project1.controller;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.TaskRequestDto;
import com.example.project1.dto.TaskResponseDto;
import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Task;
import com.example.project1.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 15, 10, 30, 45);
    // far in the future so @Future validation keeps passing
    private static final LocalDate DUE_DATE = LocalDate.of(2099, 2, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    void create_validRequest_returnsCreatedTask() throws Exception {
        when(taskService.create(any(TaskRequestDto.class))).thenReturn(response(10L, 1L, "Website"));

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(null))))
                // currently 200; CLAUDE.md wants 201 + Location header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.description").value("Controller tests"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2099-02-01"))
                .andExpect(jsonPath("$.assignee").value("alice"))
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.projectName").value("Website"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-15T10:30:45"));
    }

    @Test
    void create_bodyHasDifferentProjectId_usesProjectIdFromPath() throws Exception {
        when(taskService.create(any(TaskRequestDto.class))).thenReturn(response(10L, 1L, "Website"));

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(5L))))
                .andExpect(status().isOk());

        ArgumentCaptor<TaskRequestDto> captor = ArgumentCaptor.forClass(TaskRequestDto.class);
        verify(taskService).create(captor.capture());
        assertThat(captor.getValue().getProjectId()).isEqualTo(1L);
    }

    @Test
    void create_projectNotFound_returnsNotFound() throws Exception {
        when(taskService.create(any(TaskRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

        mockMvc.perform(post("/api/projects/99/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
    }

    @Test
    void create_invalidEnumValue_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Write tests\", \"status\": \"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid value 'DONE' for field 'status'. Allowed values: [TODO, IN_PROGRESS, COMPLETED]"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_textFieldsTooLong_returnsBadRequestWithFieldErrors() throws Exception {
        TaskRequestDto request = request(null);
        request.setTitle("a".repeat(256));
        request.setDescription("b".repeat(256));
        request.setAssignee("c".repeat(256));

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("title", "description", "assignee")));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_blankTitle_returnsBadRequestWithFieldError() throws Exception {
        TaskRequestDto request = request(null);
        request.setTitle("   ");

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_missingTitle_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"No title\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_dueDateInPast_returnsBadRequestWithFieldError() throws Exception {
        TaskRequestDto request = request(null);
        request.setDueDate(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a future date"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_dueDateToday_returnsBadRequest() throws Exception {
        TaskRequestDto request = request(null);
        request.setDueDate(LocalDate.now());

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));

        verifyNoInteractions(taskService);
    }

    @Test
    void create_noDueDate_returnsOk() throws Exception {
        TaskRequestDto request = request(null);
        request.setDueDate(null);
        when(taskService.create(any(TaskRequestDto.class))).thenReturn(response(10L, 1L, "Website"));

        mockMvc.perform(post("/api/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void update_dueDateInPast_returnsBadRequest() throws Exception {
        TaskRequestDto request = request(1L);
        request.setDueDate(LocalDate.now().minusDays(1));

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));

        verifyNoInteractions(taskService);
    }

    @Test
    void update_titleTooLong_returnsBadRequest() throws Exception {
        TaskRequestDto request = request(1L);
        request.setTitle("a".repeat(256));

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));

        verifyNoInteractions(taskService);
    }

    @Test
    void update_invalidPriority_returnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Write tests\", \"priority\": \"URGENT\", \"projectId\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value 'URGENT' for field 'priority'. Allowed values: [HIGH, MEDIUM, LOW]"));

        verifyNoInteractions(taskService);
    }

    @Test
    void findByProject_tasksExist_returnsPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id"));
        when(taskService.findByProject(eq(1L), any(Pageable.class))).thenReturn(new PageResponseDto<>(new PageImpl<>(List.of(
                response(10L, 1L, "Website"),
                response(11L, 1L, "Website")
        ), pageable, 3)));

        mockMvc.perform(get("/api/projects/1/tasks").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[1].id").value(11))
                .andExpect(jsonPath("$.content[0].projectId").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void findByProject_noTasks_returnsEmptyPage() throws Exception {
        when(taskService.findByProject(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 20))));

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void findByProject_noParameters_usesDefaultPageSizeAndSortById() throws Exception {
        when(taskService.findByProject(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 20))));

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(1L);
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void findByProject_sortByDueDateDesc_passesSortToService() throws Exception {
        when(taskService.findByProject(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(1, 10))));

        mockMvc.perform(get("/api/projects/1/tasks").param("page", "1").param("size", "10").param("sort", "dueDate,desc"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable(1L);
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "dueDate"));
    }

    @Test
    void findByProject_sizeAboveMaximum_isCappedAt100() throws Exception {
        when(taskService.findByProject(eq(1L), any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 100))));

        mockMvc.perform(get("/api/projects/1/tasks").param("size", "500"))
                .andExpect(status().isOk());

        assertThat(capturePageable(1L).getPageSize()).isEqualTo(100);
    }

    @Test
    void findByProject_invalidSortProperty_returnsBadRequest() throws Exception {
        // with a mocked service the real query never runs, so the mock throws what Spring Data would throw
        when(taskService.findByProject(eq(1L), any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("foo", TypeInformation.of(Task.class), List.of()));

        mockMvc.perform(get("/api/projects/1/tasks").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property 'foo'"));
    }

    @Test
    void findById_existingId_returnsTask() throws Exception {
        when(taskService.findById(10L)).thenReturn(response(10L, 1L, "Website"));

        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.projectName").value("Website"));
    }

    @Test
    void findById_missingId_returnsNotFoundWithMessage() throws Exception {
        when(taskService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 99"));
    }

    @Test
    void findById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        verifyNoInteractions(taskService);
    }

    @Test
    void update_existingId_returnsUpdatedTask() throws Exception {
        when(taskService.update(eq(10L), any(TaskRequestDto.class))).thenReturn(response(10L, 2L, "Mobile app"));

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.projectId").value(2))
                .andExpect(jsonPath("$.projectName").value("Mobile app"));
    }

    @Test
    void update_missingId_returnsNotFound() throws Exception {
        when(taskService.update(eq(99L), any(TaskRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request(1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 99"));
    }

    @Test
    void delete_existingId_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/tasks/10"))
                // currently 200; CLAUDE.md wants 204 No Content
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(taskService).delete(10L);
    }

    @Test
    void delete_missingId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found with id: 99"))
                .when(taskService).delete(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id: 99"));
    }

    private Pageable capturePageable(Long projectId) {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(taskService).findByProject(eq(projectId), captor.capture());
        return captor.getValue();
    }

    private TaskRequestDto request(Long projectId) {
        TaskRequestDto request = new TaskRequestDto();
        request.setTitle("Write tests");
        request.setDescription("Controller tests");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(DUE_DATE);
        request.setAssignee("alice");
        request.setProjectId(projectId);
        return request;
    }

    private TaskResponseDto response(Long id, Long projectId, String projectName) {
        return new TaskResponseDto(id, "Write tests", "Controller tests", TaskStatus.IN_PROGRESS,
                TaskPriority.HIGH, DUE_DATE, "alice", projectId, projectName, CREATED_AT);
    }
}

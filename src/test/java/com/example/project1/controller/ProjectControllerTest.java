package com.example.project1.controller;

import com.example.project1.dto.PageResponseDto;
import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.model.Project;
import com.example.project1.service.ProjectService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 15, 10, 30, 45);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void create_validRequest_returnsCreatedProject() throws Exception {
        when(projectService.create(any(ProjectRequestDto.class)))
                .thenReturn(new ProjectResponseDto(1L, "Website", "Company site", CREATED_AT));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Website", "Company site"))))
                // currently 200; CLAUDE.md wants 201 + Location header
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Website"))
                .andExpect(jsonPath("$.description").value("Company site"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-15T10:30:45"));
    }

    @Test
    void create_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.path").value("/api/projects"));

        verifyNoInteractions(projectService);
    }

    @Test
    void create_blankName_returnsBadRequestWithFieldError() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("   ", "Company site"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));

        verifyNoInteractions(projectService);
    }

    @Test
    void create_missingName_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Company site\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        verifyNoInteractions(projectService);
    }

    @Test
    void create_nameAndDescriptionTooLong_returnsBadRequestWithBothFieldErrors() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("a".repeat(256), "b".repeat(256)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')].message").value("size must be between 0 and 255"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'description')].message").value("size must be between 0 and 255"));

        verifyNoInteractions(projectService);
    }

    @Test
    void create_nameExactly255Characters_returnsOk() throws Exception {
        when(projectService.create(any(ProjectRequestDto.class)))
                .thenReturn(new ProjectResponseDto(1L, "a".repeat(255), null, CREATED_AT));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("a".repeat(255), null))))
                .andExpect(status().isOk());
    }

    @Test
    void create_unexpectedServiceError_returnsInternalServerErrorWithoutDetails() throws Exception {
        when(projectService.create(any(ProjectRequestDto.class)))
                .thenThrow(new IllegalStateException("database password is secret123"));

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Website", "Company site"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("secret123"))));
    }

    @Test
    void findAll_projectsExist_returnsPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("id"));
        when(projectService.findAll(any(Pageable.class))).thenReturn(new PageResponseDto<>(new PageImpl<>(List.of(
                new ProjectResponseDto(1L, "Website", "Company site", CREATED_AT),
                new ProjectResponseDto(2L, "Mobile app", null, CREATED_AT)
        ), pageable, 2)));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Website"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Mobile app"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findAll_noProjects_returnsEmptyPage() throws Exception {
        when(projectService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 20))));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void findAll_noParameters_usesDefaultPageSizeAndSortById() throws Exception {
        when(projectService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 20))));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void findAll_pageSizeAndSortGiven_passesThemToService() throws Exception {
        when(projectService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(2, 5))));

        mockMvc.perform(get("/api/projects").param("page", "2").param("size", "5").param("sort", "name,desc"))
                .andExpect(status().isOk());

        Pageable pageable = capturePageable();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "name"));
    }

    @Test
    void findAll_sizeAboveMaximum_isCappedAt100() throws Exception {
        when(projectService.findAll(any(Pageable.class)))
                .thenReturn(new PageResponseDto<>(Page.empty(PageRequest.of(0, 100))));

        mockMvc.perform(get("/api/projects").param("size", "500"))
                .andExpect(status().isOk());

        assertThat(capturePageable().getPageSize()).isEqualTo(100);
    }

    @Test
    void findAll_invalidSortProperty_returnsBadRequest() throws Exception {
        // with a mocked service the real query never runs, so the mock throws what Spring Data would throw
        when(projectService.findAll(any(Pageable.class)))
                .thenThrow(new PropertyReferenceException("foo", TypeInformation.of(Project.class), List.of()));

        mockMvc.perform(get("/api/projects").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid sort property 'foo'"));
    }

    @Test
    void findById_existingId_returnsProject() throws Exception {
        when(projectService.findById(1L))
                .thenReturn(new ProjectResponseDto(1L, "Website", "Company site", CREATED_AT));

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Website"))
                .andExpect(jsonPath("$.description").value("Company site"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-15T10:30:45"));
    }

    @Test
    void findById_missingId_returnsNotFoundWithMessage() throws Exception {
        when(projectService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
    }

    @Test
    void findById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        verifyNoInteractions(projectService);
    }

    @Test
    void update_existingId_returnsUpdatedProject() throws Exception {
        when(projectService.update(eq(1L), any(ProjectRequestDto.class)))
                .thenReturn(new ProjectResponseDto(1L, "New name", "New description", CREATED_AT));

        mockMvc.perform(put("/api/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("New name", "New description"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New name"))
                .andExpect(jsonPath("$.description").value("New description"));
    }

    @Test
    void update_missingId_returnsNotFound() throws Exception {
        when(projectService.update(eq(99L), any(ProjectRequestDto.class)))
                .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

        mockMvc.perform(put("/api/projects/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("New name", "New description"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
    }

    @Test
    void delete_existingId_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/api/projects/1"))
                // currently 200; CLAUDE.md wants 204 No Content
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(projectService).delete(1L);
    }

    @Test
    void delete_missingId_returnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Project not found with id: 99"))
                .when(projectService).delete(99L);

        mockMvc.perform(delete("/api/projects/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(projectService).findAll(captor.capture());
        return captor.getValue();
    }

    private ProjectRequestDto request(String name, String description) {
        ProjectRequestDto request = new ProjectRequestDto();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}

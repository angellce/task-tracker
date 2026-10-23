package com.example.project1.controller;

import com.example.project1.dto.ProjectRequestDto;
import com.example.project1.dto.ProjectResponseDto;
import com.example.project1.exception.ResourceNotFoundException;
import com.example.project1.service.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

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
                .andExpect(status().isBadRequest());

        verifyNoInteractions(projectService);
    }

    @Test
    void findAll_projectsExist_returnsList() throws Exception {
        when(projectService.findAll()).thenReturn(List.of(
                new ProjectResponseDto(1L, "Website", "Company site", CREATED_AT),
                new ProjectResponseDto(2L, "Mobile app", null, CREATED_AT)
        ));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Website"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Mobile app"));
    }

    @Test
    void findAll_noProjects_returnsEmptyArray() throws Exception {
        when(projectService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
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
                .andExpect(content().string("Project not found with id: 99"));
    }

    @Test
    void findById_nonNumericId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest());

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
                .andExpect(content().string("Project not found with id: 99"));
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
                .andExpect(content().string("Project not found with id: 99"));
    }

    private ProjectRequestDto request(String name, String description) {
        ProjectRequestDto request = new ProjectRequestDto();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}

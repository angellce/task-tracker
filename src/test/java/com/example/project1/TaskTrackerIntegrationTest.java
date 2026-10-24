package com.example.project1;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full application (controllers, services, JPA, validation, H2 + Flyway), nothing mocked.
// @Transactional rolls back each test. Trade-off: the whole test shares one open session,
// so lazy-loading problems cannot show up here.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskTrackerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void taskLifecycle_createFetchUpdateFilterDelete_worksEndToEnd() throws Exception {
        long projectId = createProject("Website");
        long taskId = createTask(projectId, "Write tests", "TODO", "2099-01-01");

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2099-01-01"))
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.projectName").value("Website"))
                // proves @PrePersist ran against the real database
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Write more tests", "status": "IN_PROGRESS", "priority": "HIGH", "projectId": %d}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write more tests"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(get("/api/tasks")
                        .param("status", "IN_PROGRESS")
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(taskId))
                .andExpect(jsonPath("$.content[0].title").value("Write more tests"));

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                // currently 200; CLAUDE.md wants 204 No Content
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: " + taskId));
    }

    @Test
    void filterAndPaginate_severalTasks_returnsCorrectPages() throws Exception {
        long alphaId = createProject("Alpha");
        long betaId = createProject("Beta");
        createTask(alphaId, "a-task", "TODO", null);
        createTask(alphaId, "c-task", "TODO", null);
        createTask(alphaId, "e-task", "IN_PROGRESS", null);
        createTask(betaId, "b-task", "TODO", null);
        createTask(betaId, "d-task", "TODO", null);

        // TODO tasks sorted by title: a, b, c, d -> page 1 (second page) of size 2 is c, d
        mockMvc.perform(get("/api/tasks")
                        .param("status", "TODO")
                        .param("size", "2")
                        .param("page", "1")
                        .param("sort", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("c-task"))
                .andExpect(jsonPath("$.content[0].projectName").value("Alpha"))
                .andExpect(jsonPath("$.content[1].title").value("d-task"))
                .andExpect(jsonPath("$.content[1].projectName").value("Beta"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", betaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void createProject_blankName_returnsBadRequestAndSavesNothing() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void createTask_pastDueDate_returnsBadRequest() throws Exception {
        long projectId = createProject("Website");

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Too late\", \"dueDate\": \"2020-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));
    }

    @Test
    void createTask_unknownProject_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/tasks", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Orphan\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Project not found with id: 999999"));
    }

    @Test
    void listTasks_invalidSortProperty_returnsBadRequest() throws Exception {
        // a real query, so Spring Data really throws PropertyReferenceException here
        mockMvc.perform(get("/api/tasks").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort property 'foo'"));
    }

    private long createProject(String name) throws Exception {
        String json = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\"}"))
                // currently 200; CLAUDE.md wants 201 + Location header
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long createTask(long projectId, String title, String status, String dueDate) throws Exception {
        String dueDateJson = dueDate == null ? "null" : "\"" + dueDate + "\"";
        String json = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "status": "%s", "dueDate": %s}
                                """.formatted(title, status, dueDateJson)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}

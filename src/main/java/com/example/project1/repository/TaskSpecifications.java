package com.example.project1.repository;

import com.example.project1.dto.TaskFilterDto;
import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import com.example.project1.model.Task;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    // Combines only the filters that are set; with no filters it matches every task
    public static Specification<Task> fromFilter(TaskFilterDto filter) {
        List<Specification<Task>> specs = new ArrayList<>();
        if (filter.getStatus() != null) {
            specs.add(hasStatus(filter.getStatus()));
        }
        if (filter.getPriority() != null) {
            specs.add(hasPriority(filter.getPriority()));
        }
        if (filter.getProjectId() != null) {
            specs.add(inProject(filter.getProjectId()));
        }
        if (filter.getAssignee() != null) {
            specs.add(assignedTo(filter.getAssignee()));
        }
        if (filter.getDueBefore() != null) {
            specs.add(dueBefore(filter.getDueBefore()));
        }
        return Specification.allOf(specs);
    }

    static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Task> hasPriority(TaskPriority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    // case-insensitive exact match: "Alice" finds "alice"
    static Specification<Task> assignedTo(String assignee) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("assignee")), assignee.toLowerCase());
    }

    // strictly before the given day; tasks without a due date never match
    static Specification<Task> dueBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThan(root.get("dueDate"), date);
    }
}

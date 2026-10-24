package com.example.project1.repository;

import com.example.project1.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task,Long>, JpaSpecificationExecutor<Task> {

    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    // load each task's project in the same query, so mapping projectName does not cause N+1 queries
    @Override
    @EntityGraph(attributePaths = "project")
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
}

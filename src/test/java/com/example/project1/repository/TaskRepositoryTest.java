package com.example.project1.repository;

import com.example.project1.dto.TaskFilterDto;
import com.example.project1.enumerations.TaskPriority;
import com.example.project1.enumerations.TaskStatus;
import com.example.project1.model.Project;
import com.example.project1.model.Task;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Runs the real Specification queries against H2 with the Flyway schema; each test rolls back
@DataJpaTest
class TaskRepositoryTest {

    private static final Pageable ALL_BY_ID = PageRequest.of(0, 20, Sort.by("id"));

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long alphaId;
    private Long betaId;

    @BeforeEach
    void setUp() {
        Project alpha = persistProject("Alpha");
        Project beta = persistProject("Beta");
        alphaId = alpha.getId();
        betaId = beta.getId();

        persistTask("t1", alpha, TaskStatus.TODO, TaskPriority.HIGH, "alice", LocalDate.of(2026, 11, 10));
        persistTask("t2", alpha, TaskStatus.TODO, TaskPriority.LOW, "Bob", LocalDate.of(2026, 12, 1));
        persistTask("t3", alpha, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "ALICE", null);
        persistTask("t4", beta, TaskStatus.TODO, TaskPriority.HIGH, "alice", LocalDate.of(2026, 11, 20));
        persistTask("t5", beta, TaskStatus.COMPLETED, TaskPriority.MEDIUM, "carol", LocalDate.of(2026, 10, 1));
        persistTask("t6", beta, TaskStatus.TODO, TaskPriority.HIGH, null, LocalDate.of(2027, 1, 1));

        // start each test with an empty persistence context, so queries really hit the database
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAll_noFilters_returnsAllTasks() {
        assertThat(titles(filter(null, null, null, null, null)))
                .containsExactly("t1", "t2", "t3", "t4", "t5", "t6");
    }

    @Test
    void findAll_statusFilter_returnsOnlyMatchingStatus() {
        assertThat(titles(filter(TaskStatus.TODO, null, null, null, null)))
                .containsExactly("t1", "t2", "t4", "t6");
    }

    @Test
    void findAll_priorityFilter_returnsOnlyMatchingPriority() {
        assertThat(titles(filter(null, TaskPriority.HIGH, null, null, null)))
                .containsExactly("t1", "t3", "t4", "t6");
    }

    @Test
    void findAll_projectIdFilter_returnsOnlyTasksOfThatProject() {
        assertThat(titles(filter(null, null, alphaId, null, null)))
                .containsExactly("t1", "t2", "t3");
    }

    @Test
    void findAll_unknownProjectId_returnsEmptyPage() {
        assertThat(titles(filter(null, null, 999L, null, null))).isEmpty();
    }

    @Test
    void findAll_assigneeFilterInDifferentCase_matchesCaseInsensitively() {
        assertThat(titles(filter(null, null, null, "Alice", null)))
                .containsExactly("t1", "t3", "t4");
    }

    @Test
    void findAll_dueBeforeFilter_returnsStrictlyEarlierAndSkipsTasksWithoutDueDate() {
        // t2 is due exactly on 2026-12-01 (excluded), t3 has no due date (excluded)
        assertThat(titles(filter(null, null, null, null, LocalDate.of(2026, 12, 1))))
                .containsExactly("t1", "t4", "t5");
    }

    @Test
    void findAll_combinedFilters_returnsOnlyTasksMatchingAll() {
        assertThat(titles(filter(TaskStatus.TODO, TaskPriority.HIGH, betaId, null, null)))
                .containsExactly("t4", "t6");
    }

    @Test
    void findAll_filterWithPaging_returnsRequestedPageAndCorrectTotals() {
        Page<Task> page = taskRepository.findAll(
                TaskSpecifications.fromFilter(new TaskFilterDto(TaskStatus.TODO, null, null, null, null)),
                PageRequest.of(1, 2, Sort.by("id")));

        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("t4", "t6");
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findAll_sortByProjectNameDesc_returnsBetaTasksFirst() {
        Page<Task> page = taskRepository.findAll(
                TaskSpecifications.fromFilter(new TaskFilterDto(null, TaskPriority.HIGH, null, null, null)),
                PageRequest.of(0, 20, Sort.by(Sort.Order.desc("project.name"), Sort.Order.asc("id"))));

        assertThat(page.getContent()).extracting(Task::getTitle).containsExactly("t4", "t6", "t1", "t3");
    }

    @Test
    void findAll_withFilter_loadsProjectInSameQuery() {
        Page<Task> page = taskRepository.findAll(
                TaskSpecifications.fromFilter(new TaskFilterDto(null, null, null, null, null)), ALL_BY_ID);

        // thanks to @EntityGraph the project is already loaded, not a lazy placeholder
        assertThat(page.getContent())
                .allSatisfy(task -> assertThat(Hibernate.isInitialized(task.getProject())).isTrue());
    }

    private List<String> titles(TaskFilterDto filter) {
        return taskRepository.findAll(TaskSpecifications.fromFilter(filter), ALL_BY_ID)
                .getContent().stream()
                .map(Task::getTitle)
                .toList();
    }

    private TaskFilterDto filter(TaskStatus status, TaskPriority priority, Long projectId, String assignee,
                                 LocalDate dueBefore) {
        return new TaskFilterDto(status, priority, projectId, assignee, dueBefore);
    }

    private Project persistProject(String name) {
        Project project = new Project();
        project.setName(name);
        return entityManager.persist(project);
    }

    private void persistTask(String title, Project project, TaskStatus status, TaskPriority priority,
                             String assignee, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setProject(project);
        task.setStatus(status);
        task.setPriority(priority);
        task.setAssignee(assignee);
        task.setDueDate(dueDate);
        entityManager.persist(task);
    }
}

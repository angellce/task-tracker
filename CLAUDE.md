# Task Tracker API: Instructions for Claude Code

Backend-only Spring Boot REST API for managing projects and tasks.
Read this whole file before doing anything. Follow it exactly.

---

## 1. Project facts (do not change without asking)

- Language/runtime: Java 21 language level (the build may run on a newer JDK, that is fine)
- Framework: Spring Boot 3.4.5 (do NOT upgrade it)
- Build tool: Maven, always through the wrapper: `./mvnw`
- Persistence: Spring Data JPA + Hibernate, H2 database, Flyway migrations
- Lombok is used. Annotation processing is configured explicitly in `pom.xml` (needed on JDK 23+). Keep it.
- Base package: `com.example.project1`
    - `controller`, `service`, `repository`, `model` (entities), `dto`, `enumerations` (TaskStatus, TaskPriority), `exception` (GlobalExceptionHandler, ResourceNotFoundException)
- Existing DTOs: TaskRequestDto, TaskResponseDto, ProjectRequestDto, ProjectResponseDto
- Domain: a Project has many Tasks
- Commands
    - Run: `./mvnw spring-boot:run`
    - Test: `./mvnw test`
    - Full check: `./mvnw verify`
- Git: work ONLY on branch `ai-experiment`

**Scope: backend only.** No frontend, no HTML pages, no templates, no React.
No authentication/Spring Security unless I explicitly ask. No new Maven modules.

---

## 2. How we work (MANDATORY for every task)

I am a student learning backend development. I must understand and approve everything before it changes.

### Step 1: PROPOSE (no edits yet)
Before creating or changing ANY file, present a proposal in this format:

```
PROPOSAL: Task N: <name>
1. What I will do (plain language, 3-6 sentences)
2. New concepts explained (2-3 simple sentences each, for anything new to me)
3. Files to create / change (full paths)
4. Dependencies / config changes (exact artifact and version, or "none")
5. Key code (snippets or a diff preview of the important parts)
6. Why this approach, and the alternatives I considered
7. Risks and open questions
8. How I will verify it (which commands)
Waiting for your approval. Reply "approved" or tell me what to change.
```

Reading files and running `./mvnw test` to understand the code is allowed before a proposal.
Editing, creating or deleting files is NOT allowed until I reply "approved".

### Step 2: IMPLEMENT only what was approved
Nothing extra. No bonus refactors, no renamed files, no "while I was there" fixes.
If something outside the approved scope needs to change, STOP and ask.

### Step 3: VERIFY
Run `./mvnw test` (or the command from the proposal) and show me the real result.

### Step 4: REPORT and wait for approval again
```
RESULT: Task N
- Files changed (output of `git diff --stat` plus new files)
- Test result (X tests run, failures, BUILD SUCCESS or FAILURE)
- What I should double-check myself
- Draft notes for my AI_LOG.md: (a) what I asked, (b) what you produced, (c) one thing to verify or that was wrong
Waiting for your approval to commit.
```

### Step 5: COMMIT only after I approve
- One commit per task, message format: `task N: short description`
- Never push. Never force-anything. Never switch branches.
- Never delete files without asking first.

I write my own AI_LOG.md. Do not edit it.

---

## 3. When to STOP and ask me (do not guess)

Stop immediately, ask clear plain questions, and wait for my answer when:
- A requirement or business rule is unclear (for example: may a task exist without a project?)
- There is more than one reasonable design (explain the options briefly and recommend one)
- Existing code looks wrong, inconsistent, or you would have to change code outside the current task
- A test fails and the cause is not obvious after 2 attempts. Do not keep retrying blindly.
- You need to add a dependency, edit `pom.xml`, change `application.properties`, or add a Flyway migration
- You are unsure whether a library version is compatible with Spring Boot 3.4.5. Never guess versions.
- Anything destructive could happen (deleting files, resetting git, dropping data)
- You do not understand something about the code or my request

Never make tests pass by weakening or deleting them, adding `@Disabled`, or swallowing exceptions.
If a test reveals a real bug in the production code, report it to me first. Do not silently fix it.

---

## 4. Technical rules for ALL tasks

- Use `jakarta.*` imports, never `javax.*`
- Constructor injection (Lombok `@RequiredArgsConstructor`), never field `@Autowired`
- Entities are never exposed in the API. Controllers accept and return DTOs only.
- No business logic in controllers. Logic lives in services.
- Schema changes ONLY through NEW Flyway migrations (`V2__...sql`, `V3__...sql`). Never edit an existing migration.
- Hibernate must not create or alter tables (check `spring.jpa.hibernate.ddl-auto` and tell me its current value before changing it)
- REST conventions: plural nouns, correct status codes (201 + Location header on create, 204 on delete, 400 invalid input, 404 not found)
- Tests: JUnit 5, Mockito, AssertJ (all included in `spring-boot-starter-test`). Test method names: `method_condition_expectedResult`.
- Keep code simple and readable. Prefer plain, obvious code over clever abstractions. Comment only what is not obvious.
- Before reusing a name or pattern, look at how the existing code does it and match its style.

---

## 5. Task list

Priority tiers: **A = core** (do first), **B = should have**. There are no other tasks. Do NOT start anything outside this list (no Docker, CI, OpenAPI, PostgreSQL, README work) unless I ask.
Do the tasks IN ORDER, one at a time. After each task, wait for my approval before starting the next.

### TIER A: Core

#### Task 1: Read-only review of the codebase (no changes)
- **Use:** Read, Glob, Grep, and `./mvnw test` only. Do NOT edit anything.
- **Report back:**
    1. Request flow for creating a task, step by step (controller, DTO, service, repository, database, response)
    2. Table of all endpoints: HTTP method, path, request DTO, response DTO, status codes returned
    3. Entities and their relationships; which fields are required in the Flyway V1 migration and their lengths
    4. Current value of `spring.jpa.hibernate.ddl-auto` and other settings in `application.properties`
    5. Problems you spot: missing validation, missing 404 handling, inconsistent naming, N+1 query risks, anything unclear
    6. What is currently untested
- **Done when:** I reply "approved, go to Task 2". No commit needed for this task.

#### Task 2: Unit tests for ProjectService
- **Use:** JUnit 5, Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock ProjectRepository`, `@InjectMocks ProjectService`), AssertJ. No Spring context.
- **File:** `src/test/java/com/example/project1/service/ProjectServiceTest.java`
- **Cover:** every public method of ProjectService: happy path, not-found path, and any other branch. Use `verify(...)` only where it adds meaning (for example `save` called once). Use `ArgumentCaptor` to check the entity passed to `save`.
- **Rules:** do not change production code. If something is hard to test or looks wrong, stop and tell me.
- **Done when:** `./mvnw test` is green. Commit: `task 2: ProjectService unit tests`

#### Task 3: Unit tests for TaskService
- **Use:** same tools as Task 2 (`@Mock TaskRepository`, `@Mock ProjectRepository`, `@InjectMocks TaskService`).
- **File:** `src/test/java/com/example/project1/service/TaskServiceTest.java`
- **Cover:** create task (project exists and project not found), update (fields and project change, task not found), get by id, delete (existing and missing id), correct mapping to the response DTO including status and priority enums.
- **Done when:** `./mvnw test` is green. Commit: `task 3: TaskService unit tests`

#### Task 4: Controller tests
- **Use:** `@WebMvcTest(ProjectController.class)` and `@WebMvcTest(TaskController.class)`, `MockMvc`, `@MockitoBean` for the services (Spring Boot 3.4 replaces the deprecated `@MockBean`; if it does not work, STOP and ask), `ObjectMapper` for JSON, `jsonPath` assertions.
- **Files:** `src/test/java/com/example/project1/controller/ProjectControllerTest.java` and `TaskControllerTest.java`
- **Cover:** every endpoint: status code, response JSON shape, and error cases. Verify that `GlobalExceptionHandler` turns `ResourceNotFoundException` into the correct status and body (`@WebMvcTest` loads `@ControllerAdvice` automatically).
- **Done when:** `./mvnw test` is green. Commit: `task 4: controller tests`

#### Task 5: Request validation and consistent error responses
- **Use:** `spring-boot-starter-validation` (version managed by Spring Boot, no explicit version) and Jakarta Bean Validation annotations.
- **Steps:**
    1. Ask approval before editing `pom.xml`.
    2. Read the V1 migration FIRST so validation limits match the real column lengths (do not guess).
    3. Add annotations to the request DTOs (`@NotBlank`, `@Size(max = ...)`, `@NotNull` where required). Ask me about optional rules, such as whether the due date may be in the past.
    4. Add `@Valid` to the controller parameters.
    5. Extend `GlobalExceptionHandler` to handle `MethodArgumentNotValidException` (400, with a list of field errors), `HttpMessageNotReadableException` (400, malformed JSON or an invalid enum value), and a generic fallback (500, with NO stack trace in the response).
    6. Show me the proposed error JSON format in the proposal before writing any code.
    7. Add tests for invalid input to the controller tests.
- **Done when:** `./mvnw test` is green. Commit: `task 5: validation and error handling`

### TIER B: Should have

#### Task 6: Pagination and sorting
- **Use:** Spring Data `Pageable` (JpaRepository already supports `findAll(Pageable)`), `@PageableDefault(size = 20)` on the controller method, and a maximum page size (`spring.data.web.pageable.max-page-size=100` in `application.properties`, which needs my approval).
- **Decide with me:** return `Page<Dto>` directly, or a small custom `PageResponse<T>` DTO (content, page, size, totalElements, totalPages). Recommend one and explain why (serializing `PageImpl` directly is discouraged by Spring Data).
- **Cover:** list endpoints for tasks and projects, an invalid sort property returning 400, and tests (service and controller).
- **Done when:** `./mvnw test` is green. Commit: `task 6: pagination and sorting`

#### Task 7: Filtering tasks
- **Filters (all optional, combinable):** `status`, `priority`, `projectId`, `assignee`, `dueBefore`.
- **Options to explain to me, then recommend one and wait for my choice:**
  (a) `JpaSpecificationExecutor` with Specification builders (flexible, a bit more code)
  (b) derived query methods (simple, but combinations multiply)
  (c) `@Query` with optional-parameter checks
- **Rules:** must work together with pagination and sorting from Task 6. An invalid enum value returns a clear 400 message.
- **Tests:** service tests, controller tests, and a `@DataJpaTest` repository test.
- **Done when:** `./mvnw test` is green. Commit: `task 7: task filtering`

#### Task 8: Integration tests
- **Use:** `@SpringBootTest` + `@AutoConfigureMockMvc`, H2 in memory with Flyway running (this also verifies the migrations), `@Transactional` so each test rolls back.
- **Scenario tests:** create project, create task, fetch it, update it, filter and paginate, delete it, confirm 404 afterwards. Add an invalid-input case.
- **Ask me** before adding any test-only properties file.
- **Done when:** `./mvnw verify` is green. Commit: `task 8: integration tests`

---

## 6. Quick reminder

PROPOSE, then wait for "approved", then IMPLEMENT only that, then VERIFY, then REPORT, then wait for "approved", then COMMIT.
When in doubt or when anything is unclear: STOP AND ASK.
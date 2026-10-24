# Task Tracker API

A backend-only REST API for managing **projects** and their **tasks**, built with Spring Boot.
A project has many tasks. Tasks have a status, a priority, an optional due date and an optional
assignee, and can be filtered, sorted and paginated.

## Tech stack

- Java 21 (language level; the build also runs on newer JDKs)
- Spring Boot 3.4.5 (Web, Data JPA, Validation)
- Maven, via the included wrapper (`./mvnw`)
- H2 in-memory database, Flyway migrations
- Lombok
- Tests: JUnit 5, Mockito, AssertJ, Spring MockMvc

## Running the app

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

> **Data is not persistent.** H2 runs in memory, so all projects and tasks are lost
> when the application stops or restarts.

## Running the tests

```bash
./mvnw test      # run all tests
./mvnw verify    # run all tests and build the jar
```

## Endpoints

| Method | Path | Purpose | Key query parameters |
|---|---|---|---|
| POST | `/api/projects` | Create a project | – |
| GET | `/api/projects` | List projects | `page`, `size`, `sort` |
| GET | `/api/projects/{id}` | Get one project | – |
| PUT | `/api/projects/{id}` | Replace a project | – |
| DELETE | `/api/projects/{id}` | Delete a project and its tasks | – |
| POST | `/api/projects/{projectId}/tasks` | Create a task in a project | – |
| GET | `/api/projects/{projectId}/tasks` | List a project's tasks | `page`, `size`, `sort` |
| GET | `/api/tasks` | Search tasks across all projects | `status`, `priority`, `projectId`, `assignee`, `dueBefore`, `page`, `size`, `sort` |
| GET | `/api/tasks/{id}` | Get one task | – |
| PUT | `/api/tasks/{id}` | Replace a task (body must include `projectId`) | – |
| DELETE | `/api/tasks/{id}` | Delete a task | – |

**Pagination:** `page` starts at 0, `size` defaults to 20 (max 100), and `sort` defaults to `id`
(for example `sort=dueDate,desc`). List responses look like
`{"content": [...], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}`.

**Filters** (all optional, combined with AND): `status` is `TODO`, `IN_PROGRESS` or `COMPLETED`;
`priority` is `HIGH`, `MEDIUM` or `LOW`; `assignee` matches exactly but ignores case; `dueBefore`
(`YYYY-MM-DD`) returns tasks due strictly before that day.

**Validation:** project `name` and task `title` are required. All text fields are limited to 255
characters. A task's `dueDate` must be in the future (today is rejected).

### Example: filtering tasks

```bash
curl 'localhost:8080/api/tasks?status=TODO&priority=HIGH&projectId=3&assignee=Alice&dueBefore=2100-01-01&sort=dueDate,asc'
```
```json
{"page":0,"content":[{"id":5,"title":"Write README","description":"Document all endpoints","status":"TODO",
 "priority":"HIGH","dueDate":"2099-12-01","assignee":"alice","projectId":3,"projectName":"README demo",
 "createdAt":"2026-10-24T20:15:27.81892"}],"size":20,"totalElements":1,"totalPages":1}
```

### Example: validation error

`POST /api/projects` with a blank `name` and a 256-character `description`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/projects",
  "fieldErrors": [
    { "field": "description", "message": "size must be between 0 and 255" },
    { "field": "name", "message": "must not be blank" }
  ],
  "timestamp": "2026-10-24T20:15:27.913565"
}
```

All errors (400, 404, 500) use this same JSON shape.

## Known limitations

- Create endpoints return `200` (not `201` with a `Location` header), and delete endpoints return
  `200` (not `204`).
- `PUT /api/tasks/{id}` without `projectId` in the body currently returns `500` instead of `400`.
- `GET /api/projects/{projectId}/tasks` returns an empty page (not `404`) for a project that does
  not exist.

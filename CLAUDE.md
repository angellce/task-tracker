# Task Tracker API

Spring Boot REST API for managing projects and tasks.

## Stack
- Java 21 (build runs on newer JDKs too), Spring Boot 3.4.5, Maven (use ./mvnw)
- Spring Data JPA, Flyway migrations, H2 database, Lombok

## Architecture
- Package com.example.project1: controller -> service -> repository layers
- DTOs (TaskRequestDto, TaskResponseDto, ProjectRequestDto, ProjectResponseDto) for requests and responses; entities are never exposed directly
- Project has many Tasks; enums TaskStatus and TaskPriority
- Errors handled centrally in GlobalExceptionHandler

## Commands
- Run: ./mvnw spring-boot:run
- Test: ./mvnw test

## Conventions
- No business logic in controllers
- Schema changes only via new Flyway migrations, never edit old ones
- Lombok annotation processing is configured explicitly in pom.xml (needed on JDK 23+), keep it
- Small commits, one per feature
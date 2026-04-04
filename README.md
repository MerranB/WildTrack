# WildTrack: AI-Powered Bio-Intelligence Platform

WildTrack is a cloud-native, full-stack application that transforms raw wildlife telemetry data from [Movebank.org](https://www.movebank.org) into actionable conservation insights. By leveraging Java Virtual Threads, PostGIS spatial analysis, and Generative AI, WildTrack provides researchers with a real-time window into global migration patterns.

---

## The Problem

Researchers are often overwhelmed by data noise. Movebank hosts millions of GPS data points, but analyzing specific behaviors — like deviations due to weather or entry into protected geo-fenced zones — remains a manual, time-consuming task. WildTrack automates this analysis and surfaces the insights that matter.

---

## Current State

The backend infrastructure is fully operational. The following has been built and tested:

- REST API with full CRUD operations and pagination (`/api/v1/items`)
- Layered architecture: Controller → Service → Repository
- PostgreSQL + PostGIS database with Flyway migrations
- Global exception handling using RFC 7807 `ProblemDetail`
- Input validation with field-level error responses
- OpenAPI / Swagger UI documentation at `/swagger-ui.html`
- Docker Compose setup for local development and testing
- Three-layer test pyramid: unit, slice, and repository integration tests

---

## Roadmap

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Backend infrastructure, CRUD API, test suite |  Complete |
| 2 | Movebank ingestion service using Virtual Threads |  In Progress |
| 3 | PostGIS spatial queries and geo-fencing alerts | Planned |
| 4 | React + Mapbox GL frontend with 3D track visualization | Planned |
| 5 | Spring AI natural language query interface | Planned |
| 6 | AWS deployment (ECS + RDS) with Terraform and GitHub Actions CI/CD | Planned |

---

## Tech Stack

**Backend**
- Java 25, Spring Boot 3.4.5
- Spring Data JPA + Hibernate
- PostgreSQL 16 + PostGIS 3.4
- Flyway (database migrations)
- MapStruct (entity/DTO mapping)
- Lombok
- SpringDoc OpenAPI 2.8.3

**Infrastructure**
- Docker + Docker Compose
- GitHub Actions (CI)

**Planned**
- React 19, TypeScript, Mapbox GL JS
- Spring AI
- AWS ECS + RDS
- Terraform

---

## Architecture Highlights

- **Virtual Threads** enabled for high-concurrency telemetry ingestion without thread-pool exhaustion
- **`@Transactional(readOnly = true)`** at the service class level with write-specific overrides — minimizes lock contention
- **Pagination** on all list endpoints — no unbounded queries
- **RFC 7807 ProblemDetail** for standardized, machine-readable error responses
- **Environment-variable-driven configuration** across all profiles — no secrets in source control

---

## Local Development Setup

**Prerequisites:** Docker, Java 25+

**1. Configure environment variables**
```bash
cp .env.example .env
# Fill in values in .env
```

**2. Start the database**
```bash
docker compose up db
```

**3. Run the application**
```bash
./mvnw spring-boot:run
```

**4. View API documentation**

Navigate to `http://localhost:8080/swagger-ui.html`

**5. Run tests**

Ensure the test database is running first:
```bash
docker compose up db-test
./mvnw test
```

---

## Project Structure

```
src/
├── main/
│   ├── java/com/wildtrack/
│   │   ├── controller/     # REST endpoints
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access
│   │   ├── model/          # JPA entities
│   │   ├── dto/            # API data transfer objects
│   │   ├── mapper/         # MapStruct mappers
│   │   ├── exception/      # Global exception handling
│   │   └── config/         # Spring configuration
│   └── resources/
│       ├── db/migration/   # Flyway SQL migrations
│       └── application*.yml
└── test/
    └── java/com/wildtrack/
        ├── controller/     # WebMvcTest slice tests
        ├── service/        # Mockito unit tests
        └── repository/     # DataJpaTest integration tests
```

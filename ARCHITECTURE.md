# WildTrack Architecture

This document describes the system architecture of WildTrack — both the AWS cloud infrastructure and the backend application structure.

---

## System Overview

WildTrack is a cloud-native REST API deployed on AWS. It ingests wildlife telemetry data from the Movebank API, stores it in a PostGIS-enabled PostgreSQL database, and exposes it through a set of REST endpoints including spatial queries and an AI-powered natural language interface.

---

## AWS Infrastructure

### Network Layout

All resources live inside a dedicated VPC split across two availability zones for redundancy.

```
VPC (10.0.0.0/16)
├── Public Subnets  (10.0.1.0/24, 10.0.2.0/24)  — ALB only
└── Private Subnets (10.0.3.0/24, 10.0.4.0/24)  — ECS, RDS
```

Public subnets are reachable from the internet via an Internet Gateway. Private subnets route outbound traffic (Movebank API calls, Anthropic API calls) through a NAT Gateway without being directly reachable from outside.

### Traffic Flow

```
Internet
    ↓  port 80
Application Load Balancer  (public subnet)
    ↓  port 8080
ECS Fargate Task           (private subnet)
    ↓  port 5432
RDS PostgreSQL + PostGIS   (private subnet)
```

### Security Groups

Three security groups enforce strict layered access:

| Security Group | Inbound | From |
|---------------|---------|------|
| ALB | port 80 | 0.0.0.0/0 (internet) |
| ECS | port 8080 | ALB security group only |
| RDS | port 5432 | ECS security group only |

Nothing can reach RDS directly from the internet or from the ALB. Nothing can reach ECS except through the ALB.

### Compute — ECS Fargate

The Spring Boot application runs as a Docker container on ECS Fargate. Fargate is serverless — AWS manages the underlying EC2 instances. The task definition specifies:

- **CPU:** 512 units (0.5 vCPU)
- **Memory:** 1024 MB
- **Launch type:** FARGATE
- **Network mode:** awsvpc (each task gets its own ENI)
- **Subnets:** Private subnets only
- **Image source:** ECR

### Database — RDS PostgreSQL + PostGIS

The database runs on a managed RDS `db.t3.micro` instance in the private subnet. PostGIS 3.4 is enabled, providing spatial data types and functions used for geo-fence and bounding-box queries. Flyway manages all schema migrations on application startup.

### Container Registry — ECR

Docker images are stored in ECR. The ECS task pulls the image at startup. Image scanning is enabled on push.

### Secrets Management

All credentials are stored in AWS Secrets Manager and injected into the ECS container as environment variables at runtime. No secrets are stored in the task definition, source control, or Docker image.

| Secret | Used by |
|--------|---------|
| DB_USER, DB_PASS | Spring datasource |
| ANTHROPIC_API_KEY | Spring AI |
| MOVEBANK_USERNAME, MOVEBANK_PASSWORD | Movebank HTTP client |
| EMAIL_USERNAME, EMAIL_PASSWORD | Spring Mail |
| KEY | API key interceptor |

### Logging — CloudWatch

All container stdout/stderr is streamed to a CloudWatch log group (`/ecs/wildtrack`) with a 7-day retention policy.

### Infrastructure as Code — Terraform

All AWS resources are defined in Terraform under `infrastructure/`. State is stored remotely in an S3 bucket with versioning enabled, making the infrastructure fully reproducible from any machine.

### DDoS Protection — Shield Standard

AWS Shield Standard is automatically applied to the ALB at no extra cost. It provides always-on protection against Layer 3 and Layer 4 DDoS attacks.

---

## Application Architecture

### Layer Structure

The backend follows a standard layered architecture:

```
Controller  →  Service  →  Repository  →  Database
                 ↑
              Mapper (MapStruct)
              Client (Movebank API)
              Scheduler (background jobs)
```

- **Controllers** handle HTTP concerns only — request mapping, validation, response codes
- **Services** contain all business logic and are the only layer that calls repositories
- **Repositories** are Spring Data JPA interfaces with custom PostGIS native queries
- **Mappers** convert between entities and DTOs using MapStruct
- **Schedulers** run background jobs independently of HTTP requests

### Package Structure

```
com.wildtrack/
├── controller/    # REST endpoints (MovebankController, GeoFenceController,
│                  #   NaturalLanguageQueryController, DemoController)
├── service/       # Business logic (MovebankEventService, GeoFenceService,
│                  #   GeoFenceAlertService, NaturalLanguageQueryService)
├── repository/    # Data access — JPA repositories with PostGIS spatial queries
├── model/         # JPA entities (MovebankEvent, GeoFence)
├── dto/           # API data transfer objects
├── mapper/        # MapStruct mappers (entity ↔ DTO, geometry conversion)
├── exception/     # GlobalExceptionHandler, custom exceptions
├── scheduler/     # UpdateDatabaseScheduler, GeoFenceScheduler
├── client/        # Movebank HTTP client (Spring RestClient)
├── email/         # Email alert service (Spring Mail)
├── demo/          # Geo-fence demo endpoint and service
├── analysis/      # Spatial query parameter models (SpatialQueryParams)
├── util/          # Input sanitization (SanitizationUtils)
└── config/        # Spring configuration (OpenApiConfig, MovebankConfiguration)
```

### Spatial Data

Animal tracking locations are stored as PostGIS `geometry(Point, 4326)` columns using JTS `Point` objects mapped via Hibernate Spatial. Geo-fence boundaries are stored as PostGIS `geometry(Polygon, 4326)` columns.

Spatial queries use native SQL with PostGIS functions:

- `ST_DWithin` — radius-based proximity queries
- `ST_Within` + `ST_MakeEnvelope` — bounding box queries
- `ST_Within` + polygon — geo-fence containment checks
- `DISTINCT ON (individual_id)` — latest position per animal

### AI Integration

The natural language query pipeline:

```
User prompt
    ↓  SanitizationUtils.sanitizeUserPrompt()
    ↓  Spring AI ChatModel (Claude Haiku 4.5)
    ↓  Structured JSON response (lat, lon, range, startDate, endDate)
    ↓  Jackson deserializes into SpatialQueryParams
    ↓  PostGIS spatial query
    ↓  Paginated MovebankEventDto response
```

Claude receives a system prompt that constrains its output to a specific JSON schema. The service validates the response structure before deserializing.

### Ingestion Pipeline

```
@Scheduled trigger (nightly cron)
    ↓  MovebankClient fetches CSV data via Spring RestClient
    ↓  Parsed into MovebankEventDto records
    ↓  Processed in batches using Java Virtual Threads
    ↓  Duplicate detection via existsByTimestampAndLocationAndIndividualIdAndTagId
    ↓  New records saved to RDS
    ↓  Ingestion summary logged (parsed, saved, duplicates, failed)
```

Virtual Threads are enabled at the application level via `spring.threads.virtual.enabled=true`, allowing high-concurrency batch processing without thread-pool exhaustion.

### Geo-fence Alert Pipeline

```
@Scheduled trigger (nightly cron)
    ↓  GeoFenceAlertService loads all geo-fences
    ↓  For each geo-fence: PostGIS query counts distinct animals inside boundary
    ↓  If count changed AND last alert was more than 3 days ago:
         → Email alert sent via Spring Mail
         → lastAnimalCount and lastAlertSent updated
```

The 3-day cooldown prevents alert spam if an animal lingers inside a boundary across multiple nightly runs.

### Error Handling

All exceptions are handled by `GlobalExceptionHandler` using Spring's `@RestControllerAdvice`. Errors are returned as RFC 7807 `ProblemDetail` objects — a standardized, machine-readable format. Custom status codes are used for Movebank-specific errors (429 rate limit, 502 bad gateway).

### Security

- **Input sanitization** — all string inputs are HTML-stripped and length-limited via `SanitizationUtils` before reaching business logic or the AI model
- **SQL injection** — prevented by JPA parameterized queries throughout
- **Secrets** — no credentials in source control or Docker images; all injected at runtime from Secrets Manager
- **Network** — ECS and RDS are in private subnets, unreachable directly from the internet

### Spring Profiles

| Profile | Purpose |
|---------|---------|
| `localdev` | Local development with Docker Compose database |
| `test` | Integration tests with a separate test database |
| `prod` | AWS deployment — scheduler disabled, external DB via environment variables |

The scheduler is disabled in `prod` via `@ConditionalOnProperty` since the dataset is historical and will not produce new movement events.

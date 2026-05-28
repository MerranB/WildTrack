# WildTrack: AI-Powered Bio-Intelligence Platform

WildTrack is a cloud-native, fullstack application that transforms raw wildlife telemetry data from [Movebank.org](https://www.movebank.org) into actionable conservation insights. It combines Java Virtual Threads, PostGIS spatial analysis, Generative AI, and a fully automated AWS deployment to give researchers a real-time window into global migration patterns.

---

## Live API

The API is deployed on AWS and fully accessible via Swagger UI:

```
http://<alb-dns>/swagger-ui.html
```

All endpoints are documented with descriptions and pre-filled example values — no setup required to explore the API.

---

## The Problem

Researchers are often overwhelmed by data noise. Movebank hosts millions of GPS data points, but analyzing specific behaviors — like deviations due to weather or entry into protected geo-fenced zones — remains a manual, time-consuming task. WildTrack automates this analysis and surfaces the insights that matter.

---

## What's Been Built

- REST API for wildlife telemetry events with full CRUD operations and pagination
- Movebank ingestion pipeline using Java Virtual Threads for high-concurrency batch processing
- Spring RestClient integration with custom error handling for Movebank API responses (429, 502)
- Scheduled background ingestion via `@Scheduled` with configurable cron and environment-based enable flag
- Null field detection with per-record warning logging and ingestion summary (parsed, saved, duplicates, failed)
- PostGIS spatial queries — bounding box and radius-based telemetry lookups
- Geo-fence management API — define geographic boundaries and receive email alerts when animal counts change
- Natural language query endpoint — plain-English prompts translated to PostGIS spatial lookups via Claude AI
- Input sanitization — XSS and prompt injection protection across all string inputs
- Global exception handling using RFC 7807 `ProblemDetail` with standardized error responses
- Input validation with field-level error responses
- Three-layer test pyramid: unit, slice, and repository integration tests
- OpenAPI / Swagger UI with named examples and pre-filled values for every endpoint
- Multi-stage production Dockerfile — non-root user, no source code in the final image
- Full AWS deployment via Terraform — VPC, ECS Fargate, RDS, ALB, Secrets Manager, ECR, CloudWatch
- GitHub Actions CI/CD — automated build, test, push to ECR, and rolling ECS deployment on every push to `main`

---

## Roadmap

| Phase | Feature | Status |
|-------|---------|--------|
| 1 | Backend infrastructure, CRUD API, test suite | Complete |
| 2 | Movebank ingestion service using Virtual Threads | Complete |
| 3 | PostGIS spatial queries and geo-fencing alerts | Complete |
| 4 | Claude AI natural language query interface | Complete |
| 5 | AWS deployment (ECS + RDS) with Terraform and GitHub Actions CI/CD | Complete |
| 6 | React + Mapbox GL frontend — live animal location map | Planned |

---

## Tech Stack

**Backend**
- Java 25, Spring Boot 3.4.5
- Spring Data JPA + Hibernate
- Spring RestClient
- PostgreSQL 16 + PostGIS 3.4
- Flyway (database migrations)
- MapStruct (entity/DTO mapping)
- Lombok
- SpringDoc OpenAPI 2.8.3

**AI**
- Spring AI 1.0.0
- Anthropic Claude Haiku 4.5

**Infrastructure**
- AWS ECS Fargate (container orchestration)
- AWS RDS PostgreSQL (managed database)
- AWS ECR (container registry)
- AWS ALB (load balancer)
- AWS Secrets Manager (credentials at runtime)
- AWS CloudWatch (container logs)
- Terraform (infrastructure as code)
- Docker + Docker Compose (local development)
- GitHub Actions (CI/CD)

**Planned**
- React 19, TypeScript, Mapbox GL JS

---

## Architecture Highlights

**Backend**
- **Virtual Threads** enabled for high-concurrency telemetry ingestion without thread-pool exhaustion
- **Spring RestClient** replaces manual HttpClient — connection pool resilience and Spring-idiomatic error handling
- **Configurable scheduler** — cron expression and enable flag externalized to configuration, togglable per environment via `@ConditionalOnProperty`
- **`@Transactional(readOnly = true)`** at the service class level with write-specific overrides — minimizes lock contention
- **Pagination** on all list endpoints — no unbounded queries
- **RFC 7807 ProblemDetail** for standardized, machine-readable error responses
- **Input sanitization** — HTML stripping and length limiting on all string inputs before processing or passing to AI
- **Environment-variable-driven configuration** across all profiles — no secrets in source control

**Infrastructure**
- **Private subnets** — ECS tasks and RDS run in private subnets, only the ALB is publicly accessible
- **Security groups** — strict layered access: internet → ALB → ECS → RDS only
- **Secrets Manager** — all credentials injected as environment variables at container startup, never stored in task definitions or source control
- **Terraform remote state** — stored in S3 with versioning, reproducible from any machine
- **Shield Standard** — automatically applied to the ALB, protecting against Layer 3/4 DDoS attacks

---

## Natural Language Query

The `GET /api/v1/analysis/query` endpoint accepts a plain-English question and returns paginated wildlife telemetry results. Spring AI sends the prompt to Claude Haiku 4.5, which extracts spatial and temporal parameters as structured JSON. WildTrack then executes a PostGIS spatial query against the tracking dataset.

**Example queries**

| Query | What Claude extracts |
|-------|----------------------|
| `"Show me all sightings near the British Virgin Islands in 2015"` | BVI coordinates · 2015-01-01 to 2015-12-31 |
| `"Find tracking events near Puerto Rico after June 2014"` | Puerto Rico coordinates · 2014-06-01 to 2016-12-31 |
| `"Where were the frigatebirds in the Caribbean in early 2016?"` | Caribbean centre · 2016-01-01 to 2016-03-31 |
| `"Show activity near Tortola between 2014 and 2015"` | Tortola coordinates · 2014-01-01 to 2015-12-31 |

> **Dataset scope:** Magnificent Frigatebird GPS tracking, British Virgin Islands and surrounding Caribbean waters, 2014–2016.

---

## Geo-fence Demo

The `POST /api/v1/demo` endpoint demonstrates the full geo-fence alert pipeline using the production code path — nothing is faked or shortcut. It creates a temporary geo-fence, inserts a real animal tracking event inside its boundary, and schedules the production geo-fence scheduler to run in 2 minutes. The scheduler performs a real PostGIS spatial query, detects the animal count change, and fires a live alert email. The geo-fence and simulated data are automatically cleaned up after the check completes.

> The geo-fence scheduled checker is disabled in the production environment as the dataset (2014–2016) is historical and will never produce new movement events. The demo compresses the nightly scheduler into a 2-minute window to make the feature demonstrable.

---

## AWS Architecture

```
Internet
    ↓
Application Load Balancer  (public subnet, Shield Standard)
    ↓  port 80
ECS Fargate Task  (private subnet)
  — image pulled from ECR on startup
  — secrets pulled from Secrets Manager on startup
  — Spring Boot app on port 8080
    ↓  port 5432
RDS PostgreSQL + PostGIS  (private subnet)
```

---

## Local Development Setup

**Prerequisites:** Docker, Java 25+, Maven

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
mvn spring-boot:run
```

**4. View API documentation**

Navigate to `http://localhost:8080/swagger-ui.html`

**5. Run tests**

Ensure the test database is running first:
```bash
docker compose up db-test
mvn test
```

---

## Project Structure

```
├── infrastructure/          # Terraform — full AWS infrastructure as code
│   ├── main.tf              # Provider and S3 backend
│   ├── vpc.tf               # VPC, subnets, NAT gateway
│   ├── security_groups.tf   # Layered security groups
│   ├── ecs.tf               # ECS cluster, task definition, service
│   ├── rds.tf               # RDS PostgreSQL instance
│   ├── alb.tf               # Application Load Balancer
│   ├── ecr.tf               # Container registry
│   ├── iam.tf               # ECS task roles
│   └── secrets.tf           # Secrets Manager entries
├── src/
│   ├── main/
│   │   ├── java/com/wildtrack/
│   │   │   ├── controller/  # REST endpoints
│   │   │   ├── service/     # Business logic
│   │   │   ├── repository/  # Data access + PostGIS queries
│   │   │   ├── model/       # JPA entities
│   │   │   ├── dto/         # API data transfer objects
│   │   │   ├── mapper/      # MapStruct mappers
│   │   │   ├── exception/   # Global exception handling
│   │   │   ├── scheduler/   # Scheduled ingestion and geo-fence jobs
│   │   │   ├── client/      # Movebank HTTP client
│   │   │   ├── email/       # Email alert service
│   │   │   ├── demo/        # Geo-fence demo endpoint
│   │   │   ├── analysis/    # Spatial query parameter models
│   │   │   ├── util/        # Input sanitization
│   │   │   └── config/      # Spring configuration
│   │   └── resources/
│   │       ├── db/migration/ # Flyway SQL migrations
│   │       └── application*.yml
│   └── test/
│       └── java/com/wildtrack/
│           ├── controller/  # WebMvcTest slice tests
│           ├── service/     # Mockito unit tests
│           ├── scheduler/   # Scheduler annotation tests
│           └── exception/   # GlobalExceptionHandler tests
└── Dockerfile               # Multi-stage production build
```

---

## Data Attribution

Animal tracking data from "[Magnificent Frigatebird_BVI_GPS-PTT_2014-2016](https://www.movebank.org/cms/webapp?gwt_fragment=page=studies,path=study19186107)"
by Jodice, P.G.R., K. Meyer, S. Zaluski, and L. Soanes. Acknowledgements: RSPB,
National Parks Trust of the Virgin Islands and BVI Department of Conservation & Fisheries.
Licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
Data ingested and transformed for use in WildTrack application.

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.